package com.merchlens;

import com.google.inject.Provides;
import com.google.gson.Gson;
import com.merchlens.model.RecommendationDto;
import com.merchlens.model.ItemSearchResult;
import com.merchlens.model.SignalResponse;
import com.merchlens.model.TimeseriesPoint;
import com.merchlens.ui.MerchLensPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Merch Lens",
	description = "Passive Grand Exchange market research for OSRS flipping.",
	tags = {"grand exchange", "flipping", "merching", "profit", "prices"}
)
public class MerchLensPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

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

	private OfferTracker offerTracker;
	private GeOfferOverlay geOfferOverlay;
	private FlipHistoryStore flipHistoryStore;
	private FavoriteItemStore favoriteItemStore;
	private MerchLensPanel panel;
	private NavigationButton navigationButton;
	private volatile long refreshSequence;
	private final Set<Integer> visibleTrendWarmInFlight = Collections.synchronizedSet(new HashSet<>());

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
		geOfferOverlay = new GeOfferOverlay(offerTracker, itemManager);
		overlayManager.add(geOfferOverlay);
		flipHistoryStore = new FlipHistoryStore(configManager, gson);
		flipHistoryStore.load();
		favoriteItemStore = new FavoriteItemStore(configManager, gson);
		favoriteItemStore.load();
		panel = new MerchLensPanel(
			this::refreshRecommendations,
			this::excludeFlip,
			this::searchItem,
			this::toggleFavorite,
			this::showDailyChart,
			this::warmVisibleTrendSignals,
			this::updateBankSize,
			config.budget(),
			this::updateScreenerFilters,
			config.screenerMinPrice(),
			config.screenerMaxPrice(),
			config.screenerMinBuyVolume(),
			config.screenerMinSellVolume(),
			config.screenerBuySellRatio(),
			itemManager
		);
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
		if (geOfferOverlay != null)
		{
			overlayManager.remove(geOfferOverlay);
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		if (offerTracker != null)
		{
			offerTracker.clear();
		}
		flipHistoryStore = null;
		favoriteItemStore = null;
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
		OfferSnapshot snapshot = offerTracker.record(event.getSlot(), event.getOffer());
		FlipHistoryStore currentStore = flipHistoryStore;
		if (currentStore != null)
		{
			currentStore.record(snapshot);
		}
	}

	private void refreshRecommendations()
	{
		MerchLensPanel currentPanel = panel;
		if (currentPanel == null)
		{
			return;
		}

		currentPanel.showLoading();
		FlipHistoryStore currentStore = flipHistoryStore;
		long sequence = ++refreshSequence;
		// Network work stays off the client thread. The result is marshalled back through Swing in the panel.
		Thread worker = new Thread(() -> {
			try
			{
				SignalResponse response = loadSignals(currentStore);
				if (!isCurrentRefresh(sequence, currentPanel))
				{
					return;
				}
				currentPanel.showRecommendations(response);
				warmHistoryAndRefresh(sequence, currentPanel, currentStore, response);
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

	private SignalResponse loadSignals(FlipHistoryStore currentStore) throws Exception
	{
		return currentStore == null
			? wikiMarketClient.getMarketSignals(
				config,
				offerTracker == null ? java.util.Collections.emptyList() : offerTracker.recent(),
				java.util.Collections.emptyList(),
				favoriteItemStore == null ? java.util.Collections.emptySet() : favoriteItemStore.itemIds(),
				new com.merchlens.model.FlipHistorySummary(0, 0, 0, 0, 0, 0),
				java.util.Collections.emptyList()
			)
			: wikiMarketClient.getMarketSignals(
				config,
				offerTracker == null ? java.util.Collections.emptyList() : offerTracker.recent(),
				currentStore.openFlips(),
				favoriteItemStore == null ? java.util.Collections.emptySet() : favoriteItemStore.itemIds(),
				currentStore.summary(),
				currentStore.recentClosed()
			);
	}

	private void warmHistoryAndRefresh(long sequence, MerchLensPanel currentPanel, FlipHistoryStore currentStore, SignalResponse response)
	{
		try
		{
			int warmed = wikiMarketClient.warmHistoricalSignals(response.getHighVolumeRecommendations(), response.getRecommendations(), response.getFavoriteRecommendations());
			if (warmed <= 0 || !isCurrentRefresh(sequence, currentPanel))
			{
				return;
			}
			SignalResponse refreshed = loadSignals(currentStore);
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
		FlipHistoryStore currentStore = flipHistoryStore;
		Thread worker = new Thread(() -> {
			try
			{
				int warmed = wikiMarketClient.warmHistoricalSignals(toWarm);
				if (warmed > 0 && isCurrentRefresh(sequence, currentPanel))
				{
					currentPanel.showRecommendations(loadSignals(currentStore));
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

	private boolean isCurrentRefresh(long sequence, MerchLensPanel currentPanel)
	{
		return sequence == refreshSequence && panel == currentPanel;
	}

	private void excludeFlip(String recordId)
	{
		FlipHistoryStore currentStore = flipHistoryStore;
		if (currentStore != null)
		{
			currentStore.exclude(recordId);
		}
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

	private void updateBankSize(int bankSize)
	{
		configManager.setConfiguration("merchlens", "budget", Math.max(1, bankSize));
		refreshRecommendations();
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

	private void showDailyChart(RecommendationDto recommendation)
	{
		MerchLensPanel currentPanel = panel;
		if (currentPanel == null || recommendation == null || recommendation.getItemId() <= 0)
		{
			return;
		}
		currentPanel.showDailyChartLoading(recommendation.getItemName());
		Thread worker = new Thread(() -> {
			try
			{
				List<TimeseriesPoint> points = wikiMarketClient.dailyTimeseries(recommendation.getItemId());
				if (panel == currentPanel)
				{
					currentPanel.showDailyChart(recommendation.getItemName(), points);
				}
			}
			catch (Exception ex)
			{
				log.debug("Unable to load Merch Lens daily chart", ex);
				if (panel == currentPanel)
				{
					currentPanel.showDailyChartError(recommendation.getItemName(), ex.getMessage());
				}
			}
		}, "merch-lens-daily-chart");
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
		graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(32, 30, 25));
		graphics.fillRoundRect(0, 0, 16, 16, 4, 4);
		graphics.setColor(new Color(124, 82, 16));
		graphics.fillOval(3, 9, 10, 4);
		graphics.fillOval(3, 6, 10, 4);
		graphics.fillOval(3, 3, 10, 4);
		graphics.setColor(new Color(225, 171, 38));
		graphics.fillOval(3, 8, 10, 4);
		graphics.fillOval(3, 5, 10, 4);
		graphics.fillOval(3, 2, 10, 4);
		graphics.setColor(new Color(255, 224, 116));
		graphics.drawArc(5, 3, 5, 2, 20, 130);
		graphics.drawArc(5, 6, 5, 2, 20, 130);
		graphics.drawArc(5, 9, 5, 2, 20, 130);
		graphics.dispose();
		return image;
	}
}
