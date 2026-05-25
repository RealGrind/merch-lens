package com.merchlens;

import com.google.inject.Provides;
import com.google.gson.Gson;
import com.merchlens.model.RecommendationDto;
import com.merchlens.model.ItemSearchResult;
import com.merchlens.model.SignalResponse;
import com.merchlens.model.TimeseriesPoint;
import com.merchlens.ui.MerchLensPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

@Slf4j
@PluginDescriptor(
	name = "Merch Lens",
	description = "Passive Grand Exchange market research for OSRS flipping.",
	tags = {"grand exchange", "flipping", "merching", "profit", "prices"}
)
public class MerchLensPlugin extends Plugin
{
	private static final String MENU_OPTION_MERCH = "Merch";
	private static final int SCREENER_TREND_BATCH_SIZE = 18;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private MerchLensConfig config;

	@Inject
	private WikiMarketClient wikiMarketClient;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TooltipManager tooltipManager;

	private OfferTracker offerTracker;
	private FlipLogStore flipLogStore;
	private GeOfferOverlay geOfferOverlay;
	private FavoriteItemStore favoriteItemStore;
	private MerchLensPanel panel;
	private NavigationButton navigationButton;
	private volatile long refreshSequence;
	private volatile long chartSequence;
	private final Set<Integer> visibleTrendWarmInFlight = Collections.synchronizedSet(new HashSet<>());
	private final Object screenerTrendScanLock = new Object();
	private volatile long screenerTrendScanSequence;
	private String runningScreenerTrendScanKey;
	private String completedScreenerTrendScanKey;

	@Provides
	MerchLensConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MerchLensConfig.class);
	}

	@Override
	protected void startUp()
	{
		offerTracker = new OfferTracker(configManager, gson);
		offerTracker.load();
		flipLogStore = new FlipLogStore(configManager, gson);
		flipLogStore.load();
		geOfferOverlay = new GeOfferOverlay(offerTracker, itemManager, config, client, tooltipManager);
		overlayManager.add(geOfferOverlay);
		favoriteItemStore = new FavoriteItemStore(configManager, gson);
		favoriteItemStore.load();
		panel = new MerchLensPanel(
			this::refreshRecommendations,
			this::searchItem,
			this::toggleFavorite,
			this::showChart,
			this::warmVisibleTrendSignals,
			this::scanScreenerTrends,
			this::cancelScreenerTrendScan,
			this::resetFlipLog,
			this::updateControlsCollapsed,
			config.controlsCollapsed(),
			this::updateScreenerFilters,
			config.screenerMinPrice(),
			config.screenerMaxPrice(),
			config.screenerMinBuyVolume(),
			config.screenerMinSellVolume(),
			config.screenerBuySellRatio(),
			itemManager
		);
		panel.setFlipLogRecords(flipLogStore.records());
		BufferedImage icon = createFallbackIcon();
		navigationButton = NavigationButton.builder()
			.tooltip("Merch Lens")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		loadSearchIndex();
		refreshRecommendations();
	}

	@Override
	protected void shutDown()
	{
		cancelScreenerTrendScan();
		if (geOfferOverlay != null)
		{
			overlayManager.remove(geOfferOverlay);
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		favoriteItemStore = null;
		flipLogStore = null;
		offerTracker = null;
		geOfferOverlay = null;
		panel = null;
		navigationButton = null;
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (event == null || event.getOffer() == null)
		{
			return;
		}
		if (offerTracker == null)
		{
			return;
		}
		if (event.getOffer().getState() == GrandExchangeOfferState.EMPTY && client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		OfferFill fill = offerTracker.record(event.getSlot(), event.getOffer());
		if (fill != null && flipLogStore != null)
		{
			ItemComposition item = itemManager.getItemComposition(fill.getItemId());
			String itemName = item == null ? null : item.getName();
			flipLogStore.record(fill, itemName);
			MerchLensPanel currentPanel = panel;
			if (currentPanel != null)
			{
				currentPanel.setFlipLogRecords(flipLogStore.records());
			}
		}
	}

	@Subscribe
	@SuppressWarnings("deprecation")
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.showItemMenuLookup() || event == null || event.getMenuEntry() == null)
		{
			return;
		}

		MenuAction type = event.getMenuEntry().getType();
		boolean widgetItemExamine = type == MenuAction.CC_OP_LOW_PRIORITY && "Examine".equals(event.getOption());
		if (type != MenuAction.EXAMINE_ITEM && type != MenuAction.EXAMINE_ITEM_GROUND && !widgetItemExamine)
		{
			return;
		}

		int itemId = type == MenuAction.EXAMINE_ITEM_GROUND ? event.getIdentifier() : event.getItemId();
		if (itemId <= 0 && event.getMenuEntry().getWidget() != null)
		{
			itemId = event.getMenuEntry().getWidget().getItemId();
		}
		if (itemId <= 0 && widgetItemExamine)
		{
			Widget container = client.getWidget(event.getActionParam1());
			Widget item = container == null ? null : container.getChild(event.getActionParam0());
			itemId = item == null ? itemId : item.getItemId();
		}
		if (itemId <= 0)
		{
			itemId = event.getIdentifier();
		}
		itemId = itemManager.canonicalize(itemId);
		if (itemId <= 0)
		{
			return;
		}

		ItemComposition composition = itemManager.getItemComposition(itemId);
		if (composition == null || composition.getName() == null || composition.getName().trim().isEmpty())
		{
			return;
		}

		int lookupItemId = itemId;
		String lookupName = composition.getName();
		client.getMenu().createMenuEntry(-1)
			.setOption(MENU_OPTION_MERCH)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(entry -> openLookupFromMenu(lookupItemId, lookupName));
	}

	private void openLookupFromMenu(int itemId, String itemName)
	{
		SwingUtilities.invokeLater(() -> {
			MerchLensPanel currentPanel = panel;
			if (currentPanel == null || navigationButton == null)
			{
				return;
			}
			clientToolbar.openPanel(navigationButton);
			currentPanel.lookupItem(itemId, itemName);
		});
	}

	private void refreshRecommendations()
	{
		MerchLensPanel currentPanel = panel;
		if (currentPanel == null)
		{
			return;
		}

		cancelScreenerTrendScan();
		currentPanel.showLoading();
		long sequence = ++refreshSequence;
		// Network work stays off the client thread. The result is marshalled back through Swing in the panel.
		Thread worker = new Thread(() -> {
			try
			{
				SignalResponse response = loadSignals();
				if (!isCurrentRefresh(sequence, currentPanel))
				{
					return;
				}
				currentPanel.showRecommendations(response);
				warmHistoryAndRefresh(sequence, currentPanel, response);
			}
			catch (Exception ex)
			{
				log.debug("Unable to refresh Merch Lens recommendations", ex);
				if (isCurrentRefresh(sequence, currentPanel))
				{
					currentPanel.showError("Market data unavailable: " + ex.getMessage());
				}
			}
		}, "merch-lens-refresh");
		worker.setDaemon(true);
		worker.start();
	}

	private SignalResponse loadSignals() throws Exception
	{
		return wikiMarketClient.getMarketSignals(
			config,
			favoriteItemStore == null ? java.util.Collections.emptySet() : favoriteItemStore.itemIds()
		);
	}

	private void warmHistoryAndRefresh(long sequence, MerchLensPanel currentPanel, SignalResponse response)
	{
		try
		{
			int warmed = wikiMarketClient.warmHistoricalSignals(response.getHighVolumeRecommendations(), response.getRecommendations(), response.getFavoriteRecommendations());
			if (warmed <= 0 || !isCurrentRefresh(sequence, currentPanel))
			{
				return;
			}
			SignalResponse refreshed = loadSignals();
			if (isCurrentRefresh(sequence, currentPanel))
			{
				currentPanel.showRecommendations(refreshed);
			}
		}
		catch (Exception ex)
		{
			log.debug("Unable to warm Merch Lens trend data", ex);
		}
	}

	private void warmVisibleTrendSignals(List<RecommendationDto> recommendations)
	{
		MerchLensPanel currentPanel = panel;
		if (currentPanel == null || recommendations == null || recommendations.isEmpty())
		{
			return;
		}

		List<RecommendationDto> toWarm = new ArrayList<>();
		for (RecommendationDto recommendation : recommendations)
		{
			if (recommendation == null || recommendation.getItemId() <= 0)
			{
				continue;
			}
			if (visibleTrendWarmInFlight.add(recommendation.getItemId()))
			{
				toWarm.add(recommendation);
			}
		}
		if (toWarm.isEmpty())
		{
			return;
		}

		long sequence = refreshSequence;
		Thread worker = new Thread(() -> {
			try
			{
				int warmed = wikiMarketClient.warmHistoricalSignals(toWarm);
				if (warmed > 0 && isCurrentRefresh(sequence, currentPanel))
				{
					currentPanel.showRecommendations(loadSignals());
				}
			}
			catch (Exception ex)
			{
				log.debug("Unable to warm visible Merch Lens trend data", ex);
			}
			finally
			{
				for (RecommendationDto recommendation : toWarm)
				{
					visibleTrendWarmInFlight.remove(recommendation.getItemId());
				}
			}
		}, "merch-lens-visible-trends");
		worker.setDaemon(true);
		worker.start();
	}

	private void scanScreenerTrends(List<RecommendationDto> recommendations)
	{
		MerchLensPanel currentPanel = panel;
		if (currentPanel == null || recommendations == null)
		{
			return;
		}

		List<RecommendationDto> candidates = new ArrayList<>(recommendations);
		String scanKey = screenerTrendScanKey(candidates);
		long scanSequence;
		synchronized (screenerTrendScanLock)
		{
			if (scanKey.equals(runningScreenerTrendScanKey))
			{
				return;
			}
			if (scanKey.equals(completedScreenerTrendScanKey))
			{
				currentPanel.showScreenerTrendProgress(candidates.size(), candidates.size(), true);
				return;
			}
			runningScreenerTrendScanKey = scanKey;
			scanSequence = ++screenerTrendScanSequence;
		}

		List<RecommendationDto> missing = new ArrayList<>();
		for (RecommendationDto recommendation : candidates)
		{
			if (recommendation != null && needsTrendScan(recommendation))
			{
				missing.add(recommendation);
			}
		}
		int checked = candidates.size() - missing.size();
		currentPanel.showScreenerTrendProgress(checked, candidates.size(), missing.isEmpty());
		if (missing.isEmpty())
		{
			completeScreenerTrendScan(scanSequence, scanKey);
			return;
		}

		long refreshAtStart = refreshSequence;
		Thread worker = new Thread(() -> {
			int completed = checked;
			try
			{
				for (int start = 0; start < missing.size(); start += SCREENER_TREND_BATCH_SIZE)
				{
					if (!isCurrentScreenerTrendScan(scanSequence, scanKey, refreshAtStart, currentPanel))
					{
						return;
					}
					int end = Math.min(start + SCREENER_TREND_BATCH_SIZE, missing.size());
					wikiMarketClient.warmHistoricalSignals(new ArrayList<>(missing.subList(start, end)));
					completed += end - start;
					if (isCurrentScreenerTrendScan(scanSequence, scanKey, refreshAtStart, currentPanel))
					{
						currentPanel.showScreenerTrendProgress(completed, candidates.size(), false);
					}
				}
				if (!isCurrentScreenerTrendScan(scanSequence, scanKey, refreshAtStart, currentPanel))
				{
					return;
				}
				SignalResponse refreshed = loadSignals();
				if (isCurrentScreenerTrendScan(scanSequence, scanKey, refreshAtStart, currentPanel))
				{
					completeScreenerTrendScan(scanSequence, scanKey);
					currentPanel.showRecommendations(refreshed);
					currentPanel.showScreenerTrendProgress(candidates.size(), candidates.size(), true);
				}
			}
			catch (Exception ex)
			{
				log.debug("Unable to complete Merch Lens Screener trend scan", ex);
			}
		}, "merch-lens-screener-trends");
		worker.setDaemon(true);
		worker.start();
	}

	private boolean needsTrendScan(RecommendationDto recommendation)
	{
		return recommendation.getMarketState() == null || "UNKNOWN".equals(recommendation.getMarketState());
	}

	private String screenerTrendScanKey(List<RecommendationDto> recommendations)
	{
		List<Integer> itemIds = new ArrayList<>();
		for (RecommendationDto recommendation : recommendations)
		{
			if (recommendation != null)
			{
				itemIds.add(recommendation.getItemId());
			}
		}
		Collections.sort(itemIds);
		StringBuilder key = new StringBuilder();
		for (Integer itemId : itemIds)
		{
			key.append(itemId).append(',');
		}
		return key.toString();
	}

	private boolean isCurrentScreenerTrendScan(long scanSequence, String scanKey, long refreshAtStart, MerchLensPanel currentPanel)
	{
		synchronized (screenerTrendScanLock)
		{
			return panel == currentPanel
				&& refreshSequence == refreshAtStart
				&& screenerTrendScanSequence == scanSequence
				&& scanKey.equals(runningScreenerTrendScanKey);
		}
	}

	private void completeScreenerTrendScan(long scanSequence, String scanKey)
	{
		synchronized (screenerTrendScanLock)
		{
			if (screenerTrendScanSequence == scanSequence && scanKey.equals(runningScreenerTrendScanKey))
			{
				runningScreenerTrendScanKey = null;
				completedScreenerTrendScanKey = scanKey;
			}
		}
	}

	private void cancelScreenerTrendScan()
	{
		synchronized (screenerTrendScanLock)
		{
			screenerTrendScanSequence++;
			runningScreenerTrendScanKey = null;
			completedScreenerTrendScanKey = null;
		}
	}

	private boolean isCurrentRefresh(long sequence, MerchLensPanel currentPanel)
	{
		return sequence == refreshSequence && panel == currentPanel;
	}

	private void toggleFavorite(int itemId)
	{
		FavoriteItemStore currentStore = favoriteItemStore;
		if (currentStore != null)
		{
			currentStore.toggle(itemId);
			refreshRecommendations();
		}
	}

	private void updateControlsCollapsed(boolean collapsed)
	{
		configManager.setConfiguration("merchlens", "controlsCollapsed", collapsed);
	}

	private void resetFlipLog(long since)
	{
		if (flipLogStore == null)
		{
			return;
		}
		flipLogStore.clearCompletedSince(since);
		if (panel != null)
		{
			panel.setFlipLogRecords(flipLogStore.records());
		}
	}

	private void updateScreenerFilters(MerchLensPanel.ScreenerFilters filters)
	{
		if (filters == null)
		{
			return;
		}
		configManager.setConfiguration("merchlens", "screenerMinPrice", Math.max(0, filters.getMinPrice()));
		configManager.setConfiguration("merchlens", "screenerMaxPrice", Math.max(0, filters.getMaxPrice()));
		configManager.setConfiguration("merchlens", "screenerMinBuyVolume", Math.max(0, filters.getMinBuyVolume()));
		configManager.setConfiguration("merchlens", "screenerMinSellVolume", Math.max(0, filters.getMinSellVolume()));
		configManager.setConfiguration("merchlens", "screenerBuySellRatio", Math.max(0, filters.getBuySellRatio()));
	}

	private void searchItem(String query)
	{
		MerchLensPanel currentPanel = panel;
		if (currentPanel == null)
		{
			return;
		}
		currentPanel.showSearchLoading(query);
		Thread worker = new Thread(() -> {
			try
			{
				RecommendationDto recommendation = wikiMarketClient.searchItem(query, config);
				if (panel == currentPanel)
				{
					currentPanel.showSearchResult(recommendation);
				}
			}
			catch (Exception ex)
			{
				log.debug("Unable to search Merch Lens item", ex);
				if (panel == currentPanel)
				{
					currentPanel.showSearchError(ex.getMessage());
				}
			}
		}, "merch-lens-search");
		worker.setDaemon(true);
		worker.start();
	}

	private void showChart(RecommendationDto recommendation, MerchLensPanel.ChartPeriod period)
	{
		MerchLensPanel currentPanel = panel;
		if (currentPanel == null || recommendation == null || recommendation.getItemId() <= 0)
		{
			return;
		}
		long sequence = ++chartSequence;
		currentPanel.showChartLoading(recommendation, period);
		Thread worker = new Thread(() -> {
			try
			{
				List<TimeseriesPoint> points = period == MerchLensPanel.ChartPeriod.WEEKLY
					? wikiMarketClient.weeklyTimeseries(recommendation.getItemId())
					: wikiMarketClient.dailyTimeseries(recommendation.getItemId());
				if (panel == currentPanel && sequence == chartSequence)
				{
					currentPanel.showChart(recommendation, period, points);
				}
			}
			catch (Exception ex)
			{
				log.debug("Unable to load Merch Lens {} chart", period.label().toLowerCase(), ex);
				if (panel == currentPanel && sequence == chartSequence)
				{
					currentPanel.showChartError(recommendation, period, ex.getMessage());
				}
			}
		}, "merch-lens-" + period.label().toLowerCase() + "-chart");
		worker.setDaemon(true);
		worker.start();
	}

	private void loadSearchIndex()
	{
		MerchLensPanel currentPanel = panel;
		if (currentPanel == null)
		{
			return;
		}
		Thread worker = new Thread(() -> {
			try
			{
				List<ItemSearchResult> items = wikiMarketClient.searchIndex();
				if (panel == currentPanel)
				{
					currentPanel.setSearchItems(items);
				}
			}
			catch (Exception ex)
			{
				log.debug("Unable to load Merch Lens item search index", ex);
			}
		}, "merch-lens-search-index");
		worker.setDaemon(true);
		worker.start();
	}

	private BufferedImage createFallbackIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		drawToolbarCoin(graphics, 1, 10);
		drawToolbarCoin(graphics, 1, 6);
		drawToolbarCoin(graphics, 1, 2);
		graphics.dispose();
		return image;
	}

	private void drawToolbarCoin(Graphics2D graphics, int x, int y)
	{
		graphics.setColor(new Color(94, 56, 8));
		graphics.fillOval(x, y + 2, 14, 5);
		graphics.setColor(new Color(244, 185, 38));
		graphics.fillOval(x, y, 14, 5);
		graphics.setColor(new Color(112, 66, 10));
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawOval(x, y, 14, 5);
		graphics.setColor(new Color(255, 230, 118));
		graphics.drawArc(x + 2, y + 1, 9, 3, 20, 120);
	}
}
