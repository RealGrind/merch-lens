package com.merchlens.ui;

import com.merchlens.model.RecommendationDto;
import com.merchlens.model.FlipRecord;
import com.merchlens.GeTax;
import com.merchlens.HighVolumeItemCatalog;
import com.merchlens.model.ItemSearchResult;
import com.merchlens.model.SignalResponse;
import com.merchlens.model.TimeseriesPoint;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

public class MerchLensPanel extends PluginPanel
{
	private static final NumberFormat GP = NumberFormat.getIntegerInstance();
	private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
	private static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	private static final Font STAT_LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final Font SECTION_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
	private static final Color TAB_ACCENT = new Color(225, 171, 38);
	private static final Color PINNED_SURFACE = new Color(34, 34, 34);
	private static final Color INPUT_SURFACE = new Color(24, 24, 24);
	private static final Color INPUT_BORDER = new Color(57, 57, 57);
	private static final int CONTROL_WIDTH = 210;
	private static final int LOOKUP_CONTROL_WIDTH = 210;
	private static final int TEXT_WIDTH = 176;
	private static final int METRIC_ROW_WIDTH = 210;
	private static final int METRIC_LABEL_WIDTH = 64;
	private static final int ITEM_ICON_SIZE = 32;
	private static final int SEARCH_PANEL_HEIGHT = 100;
	private static final int CALCULATOR_PANEL_HEIGHT = 206;
	private static final int CALCULATOR_LABEL_X = 8;
	private static final int CALCULATOR_FIELD_X = 76;
	private static final int CALCULATOR_FIELD_WIDTH = LOOKUP_CONTROL_WIDTH - CALCULATOR_FIELD_X - 12;
	private static final int CALCULATOR_CLOSE_X = LOOKUP_CONTROL_WIDTH - 22 - 8;
	private static final int NAV_HEIGHT = 40;
	private static final int PINNED_HELP_HEIGHT = 48;
	private static final int CONTROL_HEIGHT = 24;
	private static final int PAGE_ROW_HEIGHT = 33;
	private static final int FILTER_CONTROLS_HEIGHT = 58;
	private static final int SCREENER_CONTROLS_HEIGHT = 208;
	private static final int FLIP_LOG_CONTROLS_HEIGHT = 124;
	private static final int SCREENER_PROGRESS_HEIGHT = 24;
	private static final int STAPLE_CONTROLS_HEIGHT = 92;
	private static final int HEART_BUTTON_SIZE = 22;
	private static final int CLEAR_BUTTON_SIZE = 22;
	private static final int TITLE_TEXT_WIDTH = TEXT_WIDTH - ITEM_ICON_SIZE - HEART_BUTTON_SIZE - 16;
	private static final int OUTDATED_PRICE_MINUTES = 10;
	private static final int RECOMMENDATION_PAGE_SIZE = 5;
	private static final int FLIP_LOG_PAGE_SIZE = 5;
	private static final int SEARCH_MIN_QUERY_LENGTH = 1;
	private static final int SEARCH_SUGGESTION_LIMIT = 80;
	private static final int SEARCH_SUGGESTION_HEIGHT = 220;
	private static final String[] STAPLE_TYPES = HighVolumeItemCatalog.categories();
	private static final String[] MARKET_SORTS = {"Flow P&L", "Limit P&L", "ROI", "Buy price", "Net margin", "Vol/hr"};
	private static final String[] FLIP_LOG_TIMEFRAMES = {"This session", "Past 1 hr", "Past 4 hr", "Past 12 hr", "Past day", "Past week", "Past month", "All time"};
	private static final String[] FLIP_LOG_SORTS = {"Most recent", "Most profitable"};
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
		.withZone(ZoneId.systemDefault());

	private final Runnable refreshCallback;
	private final Consumer<String> searchCallback;
	private final Consumer<Integer> favoriteCallback;
	private final BiConsumer<RecommendationDto, ChartPeriod> chartCallback;
	private final Consumer<List<RecommendationDto>> visibleTrendCallback;
	private final Consumer<List<RecommendationDto>> screenerTrendScanCallback;
	private final Runnable screenerTrendCancelCallback;
	private final LongConsumer flipLogResetCallback;
	private final Consumer<Boolean> controlsCollapsedCallback;
	private final Consumer<ScreenerFilters> screenerFiltersCallback;
	private final ItemManager itemManager;
	private final JPanel pinnedContent = new JPanel();
	private final JPanel content = new ScrollableContentPanel();
	private final JLabel status = new JLabel("Refresh to load OSRS Wiki market recommendations.");
	private final JLabel credit = new JLabel("Plugin Created by Real Grind");
	private final JTextField searchField = new JTextField();
	private final JTextField calculatorBuyPriceField = new JTextField();
	private final JTextField calculatorSellPriceField = new JTextField();
	private final JTextField calculatorQuantityField = new JTextField();
	private final JTextField flipLogSearchField = new JTextField();
	private final JLabel calculatorTaxEachValue = new JLabel("-");
	private final JLabel calculatorTaxValue = new JLabel("-");
	private final JLabel calculatorProfitValue = new JLabel("-");
	private final JTextField screenerMinPriceField = new JTextField();
	private final JTextField screenerMaxPriceField = new JTextField();
	private final JTextField screenerMinBuyVolumeField = new JTextField();
	private final JTextField screenerMinSellVolumeField = new JTextField();
	private final JTextField screenerBuySellRatioField = new JTextField();
	private final JLabel searchInlineMessage = new JLabel(" ");
	private final JLabel screenerTrendProgress = new JLabel(" ");
	private JLabel screenerPaginationLabel;
	private final JPopupMenu searchPopup = new JPopupMenu();
	private JScrollPane scrollPane;
	private JFrame dailyChartFrame;
	private int openChartItemId = -1;
	private final MouseWheelListener wheelListener = this::scrollPanel;
	private int staplesPage;
	private int favoritesPage;
	private int broaderPage;
	private int flipLogPage;
	private int stapleTypeIndex;
	private int stapleSortIndex;
	private int screenerSortIndex;
	private int flipLogTimeframeIndex;
	private int flipLogSortIndex;
	private boolean stablePricesOnly;
	private boolean upToDatePricesOnly;
	private boolean calculatorExpanded;
	private boolean controlsCollapsed;
	private boolean pinnedAreaMinimized;
	private boolean favoritesExpanded;
	private boolean highVolumeExpanded = true;
	private boolean broaderExpanded;
	private boolean flipLogExpanded;
	private SignalResponse lastResponse;
	private RecommendationDto searchResult;
	private String searchMessage;
	private Color searchMessageColor = Color.LIGHT_GRAY;
	private List<ItemSearchResult> searchItems = Collections.emptyList();
	private Set<Integer> favoriteItemIds = new HashSet<>();
	private List<FlipRecord> flipLogRecords = Collections.emptyList();
	private boolean selectingSearchSuggestion;
	private boolean comboPopupOpen;
	private SignalResponse deferredRecommendationResponse;
	private int deferredScrollValue;
	private int screenerMinPrice;
	private int screenerMaxPrice;
	private int screenerMinBuyVolume;
	private int screenerMinSellVolume;
	private double screenerBuySellRatio;
	private boolean screenerTrendScanInProgress;
	private boolean screenerTrendScanComplete;
	private int screenerTrendChecked;
	private int screenerTrendTotal;
	private int renderedScreenerTotal;
	private int renderedScreenerMaxPage;
	private final long panelStartedAt = Instant.now().getEpochSecond();

	public enum ChartPeriod
	{
		DAILY("Daily", "5m", 5 * 60L, 24 * 60 * 60L),
		WEEKLY("Weekly", "1h", 60 * 60L, 7 * 24 * 60 * 60L);

		private final String label;
		private final String intervalLabel;
		private final long intervalSeconds;
		private final long durationSeconds;

		ChartPeriod(String label, String intervalLabel, long intervalSeconds, long durationSeconds)
		{
			this.label = label;
			this.intervalLabel = intervalLabel;
			this.intervalSeconds = intervalSeconds;
			this.durationSeconds = durationSeconds;
		}

		public String label()
		{
			return label;
		}
	}

	public static class ScreenerFilters
	{
		private final int minPrice;
		private final int maxPrice;
		private final int minBuyVolume;
		private final int minSellVolume;
		private final double buySellRatio;

		public ScreenerFilters(int minPrice, int maxPrice, int minBuyVolume, int minSellVolume, double buySellRatio)
		{
			this.minPrice = minPrice;
			this.maxPrice = maxPrice;
			this.minBuyVolume = minBuyVolume;
			this.minSellVolume = minSellVolume;
			this.buySellRatio = buySellRatio;
		}

		public int getMinPrice()
		{
			return minPrice;
		}

		public int getMaxPrice()
		{
			return maxPrice;
		}

		public int getMinBuyVolume()
		{
			return minBuyVolume;
		}

		public int getMinSellVolume()
		{
			return minSellVolume;
		}

		public double getBuySellRatio()
		{
			return buySellRatio;
		}
	}

	public MerchLensPanel(
		Runnable refreshCallback,
		Consumer<String> searchCallback,
		Consumer<Integer> favoriteCallback,
		BiConsumer<RecommendationDto, ChartPeriod> chartCallback,
		Consumer<List<RecommendationDto>> visibleTrendCallback,
		Consumer<List<RecommendationDto>> screenerTrendScanCallback,
		Runnable screenerTrendCancelCallback,
		LongConsumer flipLogResetCallback,
		Consumer<Boolean> controlsCollapsedCallback,
		boolean controlsCollapsed,
		Consumer<ScreenerFilters> screenerFiltersCallback,
		int screenerMinPrice,
		int screenerMaxPrice,
		int screenerMinBuyVolume,
		int screenerMinSellVolume,
		double screenerBuySellRatio,
		ItemManager itemManager)
	{
		super(false);
		this.refreshCallback = refreshCallback;
		this.searchCallback = searchCallback;
		this.favoriteCallback = favoriteCallback;
		this.chartCallback = chartCallback;
		this.visibleTrendCallback = visibleTrendCallback;
		this.screenerTrendScanCallback = screenerTrendScanCallback;
		this.screenerTrendCancelCallback = screenerTrendCancelCallback;
		this.flipLogResetCallback = flipLogResetCallback;
		this.controlsCollapsedCallback = controlsCollapsedCallback;
		this.controlsCollapsed = controlsCollapsed;
		this.screenerFiltersCallback = screenerFiltersCallback;
		this.screenerMinPrice = Math.max(0, screenerMinPrice);
		this.screenerMaxPrice = Math.max(0, screenerMaxPrice);
		this.screenerMinBuyVolume = Math.max(0, screenerMinBuyVolume);
		this.screenerMinSellVolume = Math.max(0, screenerMinSellVolume);
		this.screenerBuySellRatio = Math.max(0, screenerBuySellRatio);
		this.itemManager = itemManager;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout(8, 0));
		header.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel brand = new JPanel(new BorderLayout(7, 0));
		brand.setOpaque(false);
		brand.setAlignmentX(LEFT_ALIGNMENT);

		JComponent badge = new CoinBadge();
		JPanel titleStack = new JPanel();
		titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
		titleStack.setOpaque(false);
		JLabel title = new JLabel("Merch Lens");
		title.setForeground(Color.WHITE);
		title.setFont(SECTION_FONT);
		title.setAlignmentX(LEFT_ALIGNMENT);
		JLabel subtitle = new JLabel("Beta Version");
		subtitle.setForeground(Color.GRAY);
		subtitle.setFont(STAT_LABEL_FONT);
		subtitle.setAlignmentX(LEFT_ALIGNMENT);
		JLabel verification = new JLabel("Verify Trades Manually");
		verification.setForeground(Color.GRAY);
		verification.setFont(STAT_LABEL_FONT);
		verification.setAlignmentX(LEFT_ALIGNMENT);
		titleStack.add(title);
		titleStack.add(subtitle);
		titleStack.add(verification);
		brand.add(badge, BorderLayout.WEST);
		brand.add(titleStack, BorderLayout.CENTER);

		JButton refresh = new RefreshButton();
		refresh.setToolTipText("Refresh market data.");
		refresh.addActionListener(event -> refreshCallback.run());
		JPanel refreshSlot = new JPanel(new GridBagLayout());
		refreshSlot.setOpaque(false);
		refreshSlot.add(refresh);
		header.add(brand, BorderLayout.WEST);
		header.add(refreshSlot, BorderLayout.EAST);

		status.setForeground(Color.LIGHT_GRAY);
		status.setFont(BODY_FONT);
		status.setBorder(BorderFactory.createEmptyBorder(7, 8, 0, 8));
		credit.setForeground(Color.GRAY);
		credit.setFont(STAT_LABEL_FONT);
		credit.setBorder(BorderFactory.createEmptyBorder(1, 8, 7, 8));
		searchPopup.setFocusable(false);
		searchPopup.setRequestFocusEnabled(false);
		searchPopup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent event)
			{
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
			{
				flushDeferredRecommendationRender();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent event)
			{
				flushDeferredRecommendationRender();
			}
		});
		searchField.setFont(BODY_FONT);
		stylePinnedField(searchField);
		searchField.addActionListener(event -> submitSearch());
		searchField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				flushDeferredRecommendationRender();
			}
		});
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				searchTextChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				searchTextChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				searchTextChanged();
			}
		});
		searchInlineMessage.setFont(STAT_LABEL_FONT);
		searchInlineMessage.setForeground(Color.LIGHT_GRAY);
		searchInlineMessage.setAlignmentX(LEFT_ALIGNMENT);
		screenerTrendProgress.setFont(STAT_LABEL_FONT);
		screenerTrendProgress.setForeground(Color.LIGHT_GRAY);
		screenerTrendProgress.setAlignmentX(Component.CENTER_ALIGNMENT);
		configureCalculatorField(calculatorBuyPriceField, "Buy price per item.");
		configureCalculatorField(calculatorSellPriceField, "Sell price per item.");
		configureCalculatorField(calculatorQuantityField, "Item quantity.");
		configureFlipLogSearchField();
		configureScreenerField(screenerMinPriceField, "Minimum Buy at price. Blank means no minimum.");
		configureScreenerField(screenerMaxPriceField, "Maximum Buy at price. Blank means no maximum.");
		configureScreenerField(screenerMinBuyVolumeField, "Buy volume per hour. Leave blank for no filter.");
		configureScreenerField(screenerMinSellVolumeField, "Sell volume per hour. Leave blank for no filter.");
		configureScreenerField(screenerBuySellRatioField, "Buy/sell ratio. Higher number represents more buyer than sellers. Leave blank for no filter.");
		syncScreenerFields();

		pinnedContent.setLayout(new BoxLayout(pinnedContent, BoxLayout.Y_AXIS));
		pinnedContent.setBackground(ColorScheme.DARK_GRAY_COLOR);
		pinnedContent.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARKER_GRAY_COLOR));
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(header, BorderLayout.NORTH);
		scrollPane = new WheelScrollPane(content);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		JPanel body = new JPanel(new BorderLayout());
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.add(pinnedContent, BorderLayout.NORTH);
		body.add(scrollPane, BorderLayout.CENTER);
		renderPinnedControls(null, 0, 0, 0, 0, 0, 0, 0, 0);
		bindWheelScrolling(this);
		bindWheelScrolling(body);
		bindWheelScrolling(pinnedContent);
		bindWheelScrolling(scrollPane);
		bindWheelScrolling(scrollPane.getViewport());
		bindWheelScrolling(content);
		add(body, BorderLayout.CENTER);
		JPanel footer = new JPanel();
		footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
		footer.setBackground(new Color(24, 24, 24));
		footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(72, 72, 72)));
		status.setAlignmentX(LEFT_ALIGNMENT);
		credit.setAlignmentX(LEFT_ALIGNMENT);
		footer.add(status);
		footer.add(credit);
		add(footer, BorderLayout.SOUTH);
	}

	public void showLoading()
	{
		SwingUtilities.invokeLater(() -> {
			resetScreenerTrendProgress();
			status.setText("Loading market recommendations...");
		});
	}

	public void showError(String message)
	{
		SwingUtilities.invokeLater(() -> {
			status.setText(message);
			renderPinnedControls(lastResponse, 0, 0, 0, 0, 0, 0, 0, 0);
			content.removeAll();
			content.add(emptyState("Market data unavailable", "Check your connection and refresh OSRS Wiki recommendations."));
			content.revalidate();
			content.repaint();
			bindWheelScrolling(pinnedContent);
			bindWheelScrolling(content);
		});
	}

	public void showSearchLoading(String query)
	{
		SwingUtilities.invokeLater(() -> {
			searchResult = null;
			searchMessage = "Searching " + query + "...";
			searchMessageColor = Color.LIGHT_GRAY;
			renderCurrent();
		});
	}

	public void showSearchResult(RecommendationDto recommendation)
	{
		SwingUtilities.invokeLater(() -> {
			searchResult = recommendation;
			searchMessage = null;
			renderCurrent();
		});
	}

	public void showSearchError(String message)
	{
		SwingUtilities.invokeLater(() -> {
			searchResult = null;
			searchMessage = message == null || message.trim().isEmpty() ? "Item search failed." : message;
			searchMessageColor = riskColor("ADVANCED");
			renderCurrent();
		});
	}

	public void showChartLoading(RecommendationDto recommendation, ChartPeriod period)
	{
		SwingUtilities.invokeLater(() -> {
			if (dailyChartFrame != null && recommendation != null && openChartItemId == recommendation.getItemId())
			{
				dailyChartFrame.setTitle("Loading " + period.label.toLowerCase(Locale.US) + " chart - " + safeItemName(recommendation.getItemName()));
			}
			restoreFooterStatus();
		});
	}

	public void showChart(RecommendationDto recommendation, ChartPeriod period, List<TimeseriesPoint> points)
	{
		SwingUtilities.invokeLater(() -> {
			if (recommendation != null)
			{
				openChartDialog(recommendation, period, points == null ? Collections.emptyList() : points);
			}
			restoreFooterStatus();
		});
	}

	public void showChartError(RecommendationDto recommendation, ChartPeriod period, String message)
	{
		SwingUtilities.invokeLater(() -> {
			if (dailyChartFrame != null && recommendation != null && openChartItemId == recommendation.getItemId())
			{
				dailyChartFrame.setTitle(period.label + " chart unavailable - " + safeItemName(recommendation.getItemName()));
			}
			restoreFooterStatus();
		});
	}

	public void showRecommendations(SignalResponse response)
	{
		SwingUtilities.invokeLater(() -> {
			lastResponse = response;
			int scrollValue = currentScrollValue();
			if (isPopupInteractionOpen() || isTextEditingActive())
			{
				deferredRecommendationResponse = response;
				deferredScrollValue = scrollValue;
				return;
			}
			renderRecommendations(response);
			restoreScrollPosition(scrollValue);
		});
	}

	public void showScreenerTrendProgress(int checked, int total, boolean complete)
	{
		SwingUtilities.invokeLater(() -> {
			screenerTrendChecked = Math.max(0, Math.min(checked, total));
			screenerTrendTotal = Math.max(0, total);
			screenerTrendScanComplete = complete;
			screenerTrendScanInProgress = !complete;
			updateScreenerTrendProgressText();
			updateScreenerPaginationText();
		});
	}

	public void setSearchItems(List<ItemSearchResult> items)
	{
		SwingUtilities.invokeLater(() -> {
			searchItems = items == null ? Collections.emptyList() : new ArrayList<>(items);
			updateSearchSuggestions();
		});
	}

	public void setFlipLogRecords(List<FlipRecord> records)
	{
		SwingUtilities.invokeLater(() -> {
			flipLogRecords = records == null ? Collections.emptyList() : new ArrayList<>(records);
			if (flipLogExpanded)
			{
				renderCurrent();
			}
		});
	}

	public void lookupItem(int itemId, String itemName)
	{
		SwingUtilities.invokeLater(() -> {
			searchPopup.setVisible(false);
			selectingSearchSuggestion = true;
			searchField.setText(itemName == null ? Integer.toString(itemId) : itemName);
			selectingSearchSuggestion = false;
			setSearchInlineMessage(null, Color.LIGHT_GRAY);
			searchCallback.accept(Integer.toString(itemId));
		});
	}

	private void renderCurrent()
	{
		int scrollValue = currentScrollValue();
		if (lastResponse != null)
		{
			renderRecommendations(lastResponse);
			restoreScrollPosition(scrollValue);
			return;
		}
		content.removeAll();
		if (flipLogExpanded)
		{
			List<FlipRecord> filteredRecords = filteredFlipLogRecords();
			List<FlipItemTotal> flipItems = summarizedFlipItems(filteredRecords);
			int maxPage = Math.max(0, (flipItems.size() - 1) / FLIP_LOG_PAGE_SIZE);
			flipLogPage = Math.min(flipLogPage, maxPage);
			renderPinnedControls(null, 0, 0, 0, 0, 0, 0, flipItems.size(), maxPage);
			addSearchResult();
			addFlipLogContent(filteredRecords, flipItems);
		}
		else
		{
			renderPinnedControls(null, 0, 0, 0, 0, 0, 0, 0, 0);
			addSearchResult();
		}
		content.revalidate();
		content.repaint();
		restoreScrollPosition(scrollValue);
		bindWheelScrolling(pinnedContent);
		bindWheelScrolling(content);
	}

	private void renderCurrentAtTop()
	{
		if (lastResponse != null)
		{
			renderRecommendations(lastResponse);
			restoreScrollPosition(0);
			return;
		}
		renderCurrent();
		restoreScrollPosition(0);
	}

	private void renderRecommendations(SignalResponse response)
	{
		content.removeAll();
		List<RecommendationDto> visibleRecommendations = new ArrayList<>();
		List<RecommendationDto> recommendations = response.getRecommendations();
		favoriteItemIds = new HashSet<>(response.getFavoriteItemIds());
		status.setText(updatedStatus(response.getGeneratedAt()));

			List<RecommendationDto> favoriteCandidates = response.getFavoriteRecommendations();
			List<RecommendationDto> favorites = applyRecommendationFilters(favoriteCandidates);
			int favoriteMaxPage = Math.max(0, (favorites.size() - 1) / RECOMMENDATION_PAGE_SIZE);
			favoritesPage = Math.min(favoritesPage, favoriteMaxPage);
			List<RecommendationDto> stapleCandidates = filteredAndSortedStaples(response.getHighVolumeRecommendations());
			List<RecommendationDto> staples = applyRecommendationFilters(stapleCandidates);
			int maxPage = Math.max(0, (staples.size() - 1) / RECOMMENDATION_PAGE_SIZE);
			staplesPage = Math.min(staplesPage, maxPage);
			List<RecommendationDto> broaderCandidates = new ArrayList<>(recommendations);
			List<RecommendationDto> broaderBaseCandidates = screenerBaseFilterCandidates(broaderCandidates);
			List<RecommendationDto> broader = applyStableScreenerFilter(broaderBaseCandidates);
			boolean broaderCheckingTrends = stablePricesOnly && (screenerTrendScanInProgress || hasMissingTrend(broaderBaseCandidates));
			int broaderMaxPage = Math.max(0, (broader.size() - 1) / RECOMMENDATION_PAGE_SIZE);
			broaderPage = Math.min(broaderPage, broaderMaxPage);
			List<FlipRecord> filteredFlipRecords = filteredFlipLogRecords();
			List<FlipItemTotal> flipItems = summarizedFlipItems(filteredFlipRecords);
			int flipLogMaxPage = Math.max(0, (flipItems.size() - 1) / FLIP_LOG_PAGE_SIZE);
			flipLogPage = Math.min(flipLogPage, flipLogMaxPage);

			renderPinnedControls(response, favorites.size(), favoriteMaxPage, staples.size(), maxPage, broader.size(), broaderMaxPage, flipItems.size(), flipLogMaxPage);
			addSearchResult();

			if (favoritesExpanded)
			{
				if (favorites.isEmpty())
				{
					content.add(emptyState("No favorites shown", "Favorite an item or adjust the filters above."));
				}
				else
				{
					favorites.stream()
						.skip((long) favoritesPage * RECOMMENDATION_PAGE_SIZE)
						.limit(RECOMMENDATION_PAGE_SIZE)
						.peek(visibleRecommendations::add)
						.forEach(rec -> content.add(recommendationCard(rec)));
				}
			}

			if (highVolumeExpanded)
			{
				if (staples.isEmpty())
				{
					content.add(emptyState("No high-volume items shown", "Adjust the filters above or refresh market data."));
				}
				else
				{
					staples.stream()
						.skip((long) staplesPage * RECOMMENDATION_PAGE_SIZE)
						.limit(RECOMMENDATION_PAGE_SIZE)
						.peek(visibleRecommendations::add)
						.forEach(rec -> content.add(recommendationCard(rec)));
				}
			}

			if (broaderExpanded)
			{
				if (broader.isEmpty())
				{
					content.add(broaderCheckingTrends
						? emptyState("Checking screener trends", "Stable-only needs 24h trend data. Add filters to narrow the scan.")
						: emptyState("No screener matches", "Adjust the filters above or refresh market data."));
				}
				else
				{
					broader.stream()
						.skip((long) broaderPage * RECOMMENDATION_PAGE_SIZE)
						.limit(RECOMMENDATION_PAGE_SIZE)
						.peek(visibleRecommendations::add)
						.forEach(rec -> content.add(recommendationCard(rec)));
				}
			}

			if (flipLogExpanded)
			{
				addFlipLogContent(filteredFlipRecords, flipItems);
			}

			if (recommendations.isEmpty() && !flipLogExpanded)
			{
			content.add(emptyState("No market items", "Refresh market data or adjust the current filters."));
			}

			content.revalidate();
			content.repaint();
		requestVisibleTrendWarmup(trendWarmupCandidates(visibleRecommendations, favoriteCandidates, stapleCandidates));
		requestScreenerTrendScan(broaderBaseCandidates);
		bindWheelScrolling(pinnedContent);
		bindWheelScrolling(content);
	}

	private void addFlipLogContent(List<FlipRecord> filteredRecords, List<FlipItemTotal> flipItems)
	{
		content.add(flipLogSummaryCard(filteredRecords));
		if (flipItems.isEmpty())
		{
			content.add(emptyState("No completed flips shown", "Completed buy/sell matches recorded locally will appear here."));
			return;
		}
		flipItems.stream()
			.skip((long) flipLogPage * FLIP_LOG_PAGE_SIZE)
			.limit(FLIP_LOG_PAGE_SIZE)
			.forEach(item -> content.add(flipItemCard(item)));
	}

	private List<RecommendationDto> trendWarmupCandidates(
		List<RecommendationDto> visibleRecommendations,
		List<RecommendationDto> favoriteCandidates,
		List<RecommendationDto> stapleCandidates)
	{
		List<RecommendationDto> candidates = new ArrayList<>();
		Set<Integer> seen = new HashSet<>();
		addWarmupCandidates(candidates, seen, visibleRecommendations);
		if (!stablePricesOnly)
		{
			return candidates;
		}
		if (favoritesExpanded)
		{
			addWarmupCandidates(candidates, seen, favoriteCandidates);
		}
		else if (highVolumeExpanded)
		{
			addWarmupCandidates(candidates, seen, stapleCandidates);
		}
		return candidates;
	}

	private void addWarmupCandidates(List<RecommendationDto> target, Set<Integer> seen, List<RecommendationDto> source)
	{
		for (RecommendationDto recommendation : source)
		{
			if (recommendation != null && seen.add(recommendation.getItemId()))
			{
				target.add(recommendation);
			}
		}
	}

	private void requestVisibleTrendWarmup(List<RecommendationDto> recommendations)
	{
		if (visibleTrendCallback == null || recommendations.isEmpty())
		{
			return;
		}
		List<RecommendationDto> missingTrend = new ArrayList<>();
		for (RecommendationDto recommendation : recommendations)
		{
			if (needsTrendWarmup(recommendation))
			{
				missingTrend.add(recommendation);
			}
		}
		if (!missingTrend.isEmpty())
		{
			visibleTrendCallback.accept(missingTrend);
		}
	}

	private void requestScreenerTrendScan(List<RecommendationDto> recommendations)
	{
		if (!broaderExpanded || !stablePricesOnly || screenerTrendScanCallback == null)
		{
			return;
		}
		screenerTrendScanCallback.accept(recommendations);
	}

	private boolean needsTrendWarmup(RecommendationDto recommendation)
	{
		return recommendation.getMarketState() == null || "UNKNOWN".equals(recommendation.getMarketState());
	}

	private boolean hasMissingTrend(List<RecommendationDto> recommendations)
	{
		for (RecommendationDto recommendation : recommendations)
		{
			if (needsTrendWarmup(recommendation))
			{
				return true;
			}
		}
		return false;
	}

	private void renderPinnedControls(
		SignalResponse response,
		int favoriteTotal,
		int favoriteMaxPage,
		int stapleTotal,
		int stapleMaxPage,
		int broaderTotal,
		int broaderMaxPage,
		int flipLogTotal,
		int flipLogMaxPage)
	{
		pinnedContent.removeAll();
		if (pinnedAreaMinimized)
		{
			pinnedContent.add(minimizedPinnedControls());
			pinnedContent.revalidate();
			pinnedContent.repaint();
			return;
		}
		pinnedContent.add(searchPanel());
		pinnedContent.add(utilityNav());
		if (calculatorExpanded)
		{
			pinnedContent.add(calculatorSection());
		}
		pinnedContent.add(marketNav());
		if (!controlsCollapsed && flipLogExpanded)
		{
			pinnedContent.add(pinnedHeading("Flip Log", "Locally recorded completed flips."));
			pinnedContent.add(flipLogControls());
			pinnedContent.add(paginationControls(
				flipLogTotal,
				flipLogMaxPage,
				flipLogPage,
				() -> {
					int updatedPage = Math.max(0, flipLogPage - 1);
					if (updatedPage != flipLogPage)
					{
						flipLogPage = updatedPage;
						renderCurrentAtTop();
					}
				},
				() -> {
					int updatedPage = Math.min(flipLogMaxPage, flipLogPage + 1);
					if (updatedPage != flipLogPage)
					{
						flipLogPage = updatedPage;
						renderCurrentAtTop();
					}
				}
			));
		}
		else if (response != null)
		{
			if (!controlsCollapsed && favoritesExpanded)
			{
				pinnedContent.add(pinnedHeading("Favorites", "Saved items."));
				pinnedContent.add(filterControls());
				if (favoriteTotal > 0)
				{
					pinnedContent.add(paginationControls(
						favoriteTotal,
						favoriteMaxPage,
						favoritesPage,
						() -> {
							int updatedPage = Math.max(0, favoritesPage - 1);
							if (updatedPage != favoritesPage)
							{
								favoritesPage = updatedPage;
								renderCurrentAtTop();
							}
						},
						() -> {
							int updatedPage = Math.min(favoriteMaxPage, favoritesPage + 1);
							if (updatedPage != favoritesPage)
							{
								favoritesPage = updatedPage;
								renderCurrentAtTop();
							}
						}
					));
				}
			}
			else if (!controlsCollapsed && highVolumeExpanded)
			{
				pinnedContent.add(pinnedHeading("High Volume", "Curated high-volume watchlist."));
				pinnedContent.add(filterControls());
				pinnedContent.add(stapleControls(stapleTotal, stapleMaxPage));
			}
			else if (!controlsCollapsed && broaderExpanded)
			{
				renderedScreenerTotal = broaderTotal;
				renderedScreenerMaxPage = broaderMaxPage;
				pinnedContent.add(pinnedHeading("Market Screener", "Profitable items matching your filters."));
				pinnedContent.add(filterControls());
				pinnedContent.add(screenerControls());
				pinnedContent.add(paginationControls(
					broaderTotal,
					broaderMaxPage,
					broaderPage,
					stablePricesOnly && !screenerTrendScanComplete,
					() -> {
						int updatedPage = Math.max(0, broaderPage - 1);
						if (updatedPage != broaderPage)
						{
							broaderPage = updatedPage;
							renderCurrentAtTop();
						}
					},
					() -> {
						int updatedPage = Math.min(broaderMaxPage, broaderPage + 1);
						if (updatedPage != broaderPage)
						{
							broaderPage = updatedPage;
							renderCurrentAtTop();
						}
					}
				));
			}
		}
		pinnedContent.revalidate();
		pinnedContent.repaint();
	}

	private JPanel searchPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(47, 47, 47)),
			BorderFactory.createEmptyBorder(9, 10, 9, 10)
		));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		boolean showSearchMessage = hasSearchInlineMessage();
		fixedHeight(panel, SEARCH_PANEL_HEIGHT - (showSearchMessage ? 0 : 20));

		panel.add(searchHeading());
		panel.add(Box.createVerticalStrut(8));
		panel.add(searchQueryRow());
		if (showSearchMessage)
		{
			panel.add(Box.createVerticalStrut(4));
			fixedSize(searchInlineMessage, LOOKUP_CONTROL_WIDTH, 16);
			panel.add(searchInlineMessage);
		}
		return panel;
	}

	private JComponent searchHeading()
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		flexibleRow(row, LOOKUP_CONTROL_WIDTH, CONTROL_HEIGHT);

		JLabel label = new JLabel("Item Lookup");
		label.setForeground(Color.WHITE);
		label.setFont(SECTION_FONT);
		row.add(label, BorderLayout.WEST);
		JButton minimize = new PinnedAreaButton(false);
		minimize.setToolTipText("Hide controls.");
		minimize.addActionListener(event -> togglePinnedArea());
		row.add(minimize, BorderLayout.EAST);
		return row;
	}

	private JPanel minimizedPinnedControls()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(47, 47, 47)),
			BorderFactory.createEmptyBorder(9, 10, 9, 10)
		));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, CONTROL_HEIGHT + 19);
		JButton restore = new PinnedAreaButton(true);
		restore.setToolTipText("Show controls.");
		restore.addActionListener(event -> togglePinnedArea());
		panel.add(restore, BorderLayout.EAST);
		return panel;
	}

	private void togglePinnedArea()
	{
		pinnedAreaMinimized = !pinnedAreaMinimized;
		if (pinnedAreaMinimized)
		{
			searchPopup.setVisible(false);
		}
		renderCurrent();
	}

	private JPanel searchQueryRow()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		flexibleRow(row, LOOKUP_CONTROL_WIDTH, CONTROL_HEIGHT);

		JButton search = new SearchButton();
		search.setToolTipText("Search item.");
		search.addActionListener(event -> submitSearch());
		row.add(searchInputControl(LOOKUP_CONTROL_WIDTH - 34), BorderLayout.CENTER);
		row.add(search, BorderLayout.EAST);
		return row;
	}

	private JPanel calculatorSection()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 8, 10));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, CALCULATOR_PANEL_HEIGHT + 5);
		panel.add(calculatorPanel(), BorderLayout.CENTER);
		return panel;
	}

	private void setControlsCollapsed(boolean collapsed)
	{
		if (controlsCollapsed == collapsed)
		{
			return;
		}
		controlsCollapsed = collapsed;
		if (collapsed)
		{
			searchPopup.setVisible(false);
		}
		if (controlsCollapsedCallback != null)
		{
			controlsCollapsedCallback.accept(collapsed);
		}
		renderCurrent();
	}

	private JComponent searchInputControl(int width)
	{
		searchField.setAlignmentX(LEFT_ALIGNMENT);
		if (!hasActiveSearch())
		{
			fixedSize(searchField, width, CONTROL_HEIGHT);
			return searchField;
		}

		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(row, width, CONTROL_HEIGHT);
		fixedSize(searchField, width - CLEAR_BUTTON_SIZE - 4, CONTROL_HEIGHT);
		row.add(searchField, BorderLayout.CENTER);
		row.add(clearSearchButton("Clear item lookup"), BorderLayout.EAST);
		return row;
	}

	private JPanel calculatorPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(null);
		panel.setOpaque(true);
		panel.setBackground(new Color(30, 30, 30));
		panel.setBorder(BorderFactory.createLineBorder(new Color(54, 54, 54)));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(panel, LOOKUP_CONTROL_WIDTH, CALCULATOR_PANEL_HEIGHT - 8);

		JLabel title = new JLabel("Profit Calculator");
		title.setForeground(Color.WHITE);
		title.setFont(TITLE_FONT);
		title.setBounds(8, 8, 160, 22);
		JButton close = new CloseButton();
		close.setToolTipText("Close calculator.");
		close.addActionListener(event -> toggleCalculator());
		close.setBounds(CALCULATOR_CLOSE_X, 8, CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE);

		panel.add(title);
		panel.add(close);
		addCalculatorInputRow(panel, "Buy price", calculatorBuyPriceField, 38);
		addCalculatorInputRow(panel, "Sell price", calculatorSellPriceField, 65);
		addCalculatorInputRow(panel, "Quantity", calculatorQuantityField, 92);
		addCalculatorResultRow(panel, "Tax per item", calculatorTaxEachValue, 124);
		addCalculatorResultRow(panel, "Total tax", calculatorTaxValue, 146);
		addCalculatorResultRow(panel, "Post-tax", calculatorProfitValue, 168);
		updateCalculatorResult();
		return panel;
	}

	private void addCalculatorInputRow(JPanel panel, String labelText, JTextField field, int y)
	{
		JLabel label = new JLabel(labelText);
		label.setForeground(Color.LIGHT_GRAY);
		label.setFont(STAT_LABEL_FONT);
		label.setHorizontalAlignment(JLabel.LEFT);
		label.setBounds(CALCULATOR_LABEL_X, y + 2, CALCULATOR_FIELD_X - CALCULATOR_LABEL_X - 8, 18);
		field.setBounds(CALCULATOR_FIELD_X, y, CALCULATOR_FIELD_WIDTH, 22);

		panel.add(label);
		panel.add(field);
	}

	private void addCalculatorResultRow(JPanel panel, String labelText, JLabel value, int y)
	{
		JLabel label = new JLabel(labelText);
		label.setForeground(Color.GRAY);
		label.setFont(STAT_LABEL_FONT);
		label.setHorizontalAlignment(JLabel.LEFT);
		label.setBounds(CALCULATOR_LABEL_X, y, CALCULATOR_FIELD_X - CALCULATOR_LABEL_X - 8, 18);
		value.setFont(BODY_FONT);
		value.setHorizontalAlignment(JLabel.RIGHT);
		value.setToolTipText("Uses standard GE tax: 2%, capped at 5M gp per item.");
		value.setBounds(CALCULATOR_FIELD_X, y, CALCULATOR_FIELD_WIDTH, 18);

		panel.add(label);
		panel.add(value);
	}

	private JPanel screenerControls()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createEmptyBorder(2, 10, 8, 10));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, SCREENER_CONTROLS_HEIGHT + (stablePricesOnly ? SCREENER_PROGRESS_HEIGHT : 0));
		panel.add(screenerFilterRow("Min price", screenerMinPriceField));
		panel.add(Box.createVerticalStrut(4));
		panel.add(screenerFilterRow("Max price", screenerMaxPriceField));
		panel.add(Box.createVerticalStrut(4));
		panel.add(screenerFilterRow("Buy vol/hr", screenerMinBuyVolumeField));
		panel.add(Box.createVerticalStrut(4));
		panel.add(screenerFilterRow("Sell vol/hr", screenerMinSellVolumeField));
		panel.add(Box.createVerticalStrut(4));
		panel.add(screenerFilterRow("B/S ratio", screenerBuySellRatioField));
		panel.add(Box.createVerticalStrut(6));
		panel.add(screenerButtonRow());
		panel.add(Box.createVerticalStrut(4));
		panel.add(screenerSortControl());
		if (stablePricesOnly)
		{
			panel.add(Box.createVerticalStrut(5));
			updateScreenerTrendProgressText();
			fixedSize(screenerTrendProgress, CONTROL_WIDTH, 18);
			panel.add(screenerTrendProgress);
		}
		return panel;
	}

	private JComboBox<String> screenerSortControl()
	{
		JComboBox<String> sort = new JComboBox<>(MARKET_SORTS);
		sort.setSelectedIndex(screenerSortIndex);
		stylePinnedCombo(sort, "Sort items by");
		fixedSize(sort, CONTROL_WIDTH, CONTROL_HEIGHT);
		sort.setAlignmentX(Component.CENTER_ALIGNMENT);
		deferRendersWhileOpen(sort);
		sort.addActionListener(event -> {
			screenerSortIndex = sort.getSelectedIndex();
			broaderPage = 0;
			renderCurrentAtTop();
		});
		return sort;
	}

	private JPanel flipLogControls()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createEmptyBorder(2, 10, 8, 10));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, FLIP_LOG_CONTROLS_HEIGHT);

		JComboBox<String> timeframe = new JComboBox<>(FLIP_LOG_TIMEFRAMES);
		timeframe.setSelectedIndex(flipLogTimeframeIndex);
		stylePinnedCombo(timeframe, "Timeframe for completed flips.");
		fixedSize(timeframe, CONTROL_WIDTH, CONTROL_HEIGHT);
		timeframe.setAlignmentX(Component.CENTER_ALIGNMENT);
		timeframe.addActionListener(event -> {
			flipLogTimeframeIndex = timeframe.getSelectedIndex();
			flipLogPage = 0;
			renderCurrentAtTop();
		});
		panel.add(timeframe);
		panel.add(Box.createVerticalStrut(4));
		panel.add(flipLogSearchRow());
		panel.add(Box.createVerticalStrut(4));

		JComboBox<String> sort = new JComboBox<>(FLIP_LOG_SORTS);
		sort.setSelectedIndex(flipLogSortIndex);
		stylePinnedCombo(sort, "Sort logged item totals.");
		fixedSize(sort, CONTROL_WIDTH, CONTROL_HEIGHT);
		sort.setAlignmentX(Component.CENTER_ALIGNMENT);
		sort.addActionListener(event -> {
			flipLogSortIndex = sort.getSelectedIndex();
			flipLogPage = 0;
			renderCurrentAtTop();
		});
		panel.add(sort);
		panel.add(Box.createVerticalStrut(6));

		JButton reset = new JButton("Reset log");
		reset.setFont(BODY_FONT);
		reset.setToolTipText("Clear logged completed flips in the selected timeframe.");
		fixedSize(reset, CONTROL_WIDTH, CONTROL_HEIGHT);
		reset.setAlignmentX(Component.CENTER_ALIGNMENT);
		reset.addActionListener(event -> confirmFlipLogReset());
		panel.add(reset);
		return panel;
	}

	private JPanel flipLogSearchRow()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		fixedSize(row, CONTROL_WIDTH, CONTROL_HEIGHT);
		fixedSize(flipLogSearchField, CONTROL_WIDTH - 34, CONTROL_HEIGHT);
		JButton search = new SearchButton();
		search.setToolTipText("Filter flip log.");
		search.addActionListener(event -> {
			flipLogPage = 0;
			renderCurrentAtTop();
		});
		row.add(flipLogSearchField, BorderLayout.CENTER);
		row.add(search, BorderLayout.EAST);
		return row;
	}

	private void confirmFlipLogReset()
	{
		String timeframe = FLIP_LOG_TIMEFRAMES[flipLogTimeframeIndex];
		int result = JOptionPane.showConfirmDialog(
			this,
			"Delete completed flip results from " + timeframe.toLowerCase(Locale.US) + "?\n"
				+ "Open buy cost basis is retained for later sales.",
			"Reset Flip Log",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if (result == JOptionPane.YES_OPTION && flipLogResetCallback != null)
		{
			flipLogResetCallback.accept(selectedFlipLogCutoff());
			flipLogPage = 0;
		}
	}

	private JPanel screenerButtonRow()
	{
		JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		fixedSize(row, CONTROL_WIDTH, CONTROL_HEIGHT);

		JButton filter = new JButton("Filter");
		filter.setFont(BODY_FONT);
		filter.setToolTipText("Apply Screener filters.");
		filter.addActionListener(event -> commitScreenerFilters());

		JButton reset = new JButton("Reset");
		reset.setFont(BODY_FONT);
		reset.setToolTipText("Clear Screener filters.");
		reset.addActionListener(event -> resetScreenerFilters());

		row.add(filter);
		row.add(reset);
		return row;
	}

	private JPanel screenerFilterRow(String labelText, JTextField field)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		fixedSize(row, CONTROL_WIDTH, CONTROL_HEIGHT);

		JLabel label = new JLabel(labelText);
		label.setForeground(Color.LIGHT_GRAY);
		label.setFont(BODY_FONT);
		label.setToolTipText(field.getToolTipText());
		fixedSize(label, 78, CONTROL_HEIGHT);

		fixedSize(field, CONTROL_WIDTH - 84, CONTROL_HEIGHT);
		row.add(label, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private void configureScreenerField(JTextField field, String tooltip)
	{
		field.setFont(BODY_FONT);
		field.setHorizontalAlignment(JTextField.RIGHT);
		stylePinnedField(field);
		field.setToolTipText(tooltip);
		field.addActionListener(event -> commitScreenerFilters());
		field.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				commitScreenerFilters();
				flushDeferredRecommendationRender();
			}
		});
	}

	private void stylePinnedField(JTextField field)
	{
		field.setForeground(Color.WHITE);
		field.setCaretColor(Color.WHITE);
		field.setBackground(INPUT_SURFACE);
		field.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(INPUT_BORDER),
			BorderFactory.createEmptyBorder(1, 6, 1, 6)
		));
	}

	private void configureCalculatorField(JTextField field, String tooltip)
	{
		field.setFont(BODY_FONT);
		field.setHorizontalAlignment(JTextField.RIGHT);
		field.setToolTipText(tooltip);
		field.setForeground(Color.WHITE);
		field.setCaretColor(Color.WHITE);
		field.setBackground(new Color(18, 18, 18));
		field.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(78, 78, 78)),
			BorderFactory.createEmptyBorder(1, 4, 1, 4)
		));
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				updateCalculatorResult();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				updateCalculatorResult();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				updateCalculatorResult();
			}
		});
		field.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				flushDeferredRecommendationRender();
			}
		});
	}

	private void configureFlipLogSearchField()
	{
		flipLogSearchField.setFont(BODY_FONT);
		stylePinnedField(flipLogSearchField);
		flipLogSearchField.setToolTipText("Filter logged flips by item name.");
		flipLogSearchField.addActionListener(event -> {
			flipLogPage = 0;
			renderCurrentAtTop();
		});
	}

	private void toggleCalculator()
	{
		calculatorExpanded = !calculatorExpanded;
		renderCurrent();
		if (calculatorExpanded)
		{
			SwingUtilities.invokeLater(calculatorBuyPriceField::requestFocusInWindow);
		}
	}

	private void updateCalculatorResult()
	{
		Integer buyPrice = parseGpAmount(calculatorBuyPriceField.getText());
		Integer sellPrice = parseGpAmount(calculatorSellPriceField.getText());
		Integer quantity = parseGpAmount(calculatorQuantityField.getText());
		if (buyPrice == null || sellPrice == null || quantity == null || quantity <= 0)
		{
			calculatorTaxEachValue.setText("-");
			calculatorTaxEachValue.setForeground(Color.LIGHT_GRAY);
			calculatorTaxValue.setText("-");
			calculatorTaxValue.setForeground(Color.LIGHT_GRAY);
			calculatorProfitValue.setText("-");
			calculatorProfitValue.setForeground(Color.LIGHT_GRAY);
			return;
		}

		int taxEach = GeTax.tax(sellPrice, null);
		long totalTax = (long) taxEach * quantity;
		long profitEach = (long) sellPrice - buyPrice - taxEach;
		long totalProfit = profitEach * quantity;

		calculatorTaxEachValue.setText(gp(taxEach));
		calculatorTaxEachValue.setForeground(Color.LIGHT_GRAY);
		calculatorTaxValue.setText(gp(totalTax));
		calculatorTaxValue.setForeground(Color.LIGHT_GRAY);
		calculatorProfitValue.setText(signedGp(totalProfit));
		calculatorProfitValue.setForeground(totalProfit >= 0 ? riskColor("SAFE") : riskColor("ADVANCED"));
	}

	private void commitScreenerFilters()
	{
		Integer minPrice = parseOptionalGpAmount(screenerMinPriceField.getText());
		Integer maxPrice = parseOptionalGpAmount(screenerMaxPriceField.getText());
		Integer minBuyVolume = parseOptionalGpAmount(screenerMinBuyVolumeField.getText());
		Integer minSellVolume = parseOptionalGpAmount(screenerMinSellVolumeField.getText());
		Double buySellRatio = parseOptionalRatio(screenerBuySellRatioField.getText());
		if (minPrice == null || maxPrice == null || minBuyVolume == null || minSellVolume == null || buySellRatio == null)
		{
			syncScreenerFields();
			status.setText("Enter valid Screener filters.");
			return;
		}

		if (minPrice == screenerMinPrice
			&& maxPrice == screenerMaxPrice
			&& minBuyVolume == screenerMinBuyVolume
			&& minSellVolume == screenerMinSellVolume
			&& Double.compare(buySellRatio, screenerBuySellRatio) == 0)
		{
			syncScreenerFields();
			return;
		}

		screenerMinPrice = minPrice;
		screenerMaxPrice = maxPrice;
		screenerMinBuyVolume = minBuyVolume;
		screenerMinSellVolume = minSellVolume;
		screenerBuySellRatio = buySellRatio;
		broaderPage = 0;
		resetScreenerTrendProgress();
		syncScreenerFields();
		screenerFiltersCallback.accept(new ScreenerFilters(
			screenerMinPrice,
			screenerMaxPrice,
			screenerMinBuyVolume,
			screenerMinSellVolume,
			screenerBuySellRatio
		));
		if (lastResponse != null)
		{
			showRecommendations(lastResponse);
		}
	}

	private void resetScreenerFilters()
	{
		boolean changed = screenerMinPrice != 0
			|| screenerMaxPrice != 0
			|| screenerMinBuyVolume != 0
			|| screenerMinSellVolume != 0
			|| Double.compare(screenerBuySellRatio, 0) != 0;
		screenerMinPrice = 0;
		screenerMaxPrice = 0;
		screenerMinBuyVolume = 0;
		screenerMinSellVolume = 0;
		screenerBuySellRatio = 0;
		broaderPage = 0;
		resetScreenerTrendProgress();
		syncScreenerFields();
		if (changed)
		{
			screenerFiltersCallback.accept(new ScreenerFilters(0, 0, 0, 0, 0));
		}
		if (lastResponse != null)
		{
			showRecommendations(lastResponse);
		}
	}

	private void syncScreenerFields()
	{
		screenerMinPriceField.setText(formatOptionalInt(screenerMinPrice));
		screenerMaxPriceField.setText(formatOptionalInt(screenerMaxPrice));
		screenerMinBuyVolumeField.setText(formatOptionalInt(screenerMinBuyVolume));
		screenerMinSellVolumeField.setText(formatOptionalInt(screenerMinSellVolume));
		screenerBuySellRatioField.setText(formatOptionalRatio(screenerBuySellRatio));
	}

	private void resetScreenerTrendProgress()
	{
		screenerTrendScanInProgress = false;
		screenerTrendScanComplete = false;
		screenerTrendChecked = 0;
		screenerTrendTotal = 0;
		updateScreenerTrendProgressText();
	}

	private void updateScreenerTrendProgressText()
	{
		if (!stablePricesOnly)
		{
			screenerTrendProgress.setText(" ");
			return;
		}
		if (screenerTrendScanInProgress)
		{
			screenerTrendProgress.setText("Checking stability: " + screenerTrendChecked + " / " + screenerTrendTotal);
			return;
		}
		if (screenerTrendScanComplete)
		{
			screenerTrendProgress.setText("Stability checked: " + screenerTrendTotal + " items");
			return;
		}
		screenerTrendProgress.setText("Preparing stability scan...");
	}

	private void updateScreenerPaginationText()
	{
		if (screenerPaginationLabel == null || !broaderExpanded)
		{
			return;
		}
		boolean incomplete = stablePricesOnly && !screenerTrendScanComplete;
		String count = incomplete ? renderedScreenerTotal + " so far" : Integer.toString(renderedScreenerTotal);
		screenerPaginationLabel.setText((broaderPage + 1) + "/" + (renderedScreenerMaxPage + 1) + " (" + count + ")");
		screenerPaginationLabel.revalidate();
		screenerPaginationLabel.repaint();
	}

	private Integer parseGpAmount(String text)
	{
		if (text == null)
		{
			return null;
		}
		String digits = text.replaceAll("[^0-9]", "");
		if (digits.isEmpty())
		{
			return null;
		}
		try
		{
			long value = Long.parseLong(digits);
			return (int) Math.min(Integer.MAX_VALUE, value);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private Integer parseOptionalGpAmount(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return 0;
		}
		return parseGpAmount(text);
	}

	private Double parseOptionalRatio(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return 0.0;
		}
		String cleaned = text.trim().replace(",", "");
		boolean percent = cleaned.endsWith("%");
		if (percent)
		{
			cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
		}
		try
		{
			double value = Double.parseDouble(cleaned);
			if (percent)
			{
				value /= 100.0;
			}
			if (value < 0)
			{
				return null;
			}
			return value;
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private String formatOptionalInt(int value)
	{
		return value <= 0 ? "" : GP.format(value);
	}

	private String formatOptionalRatio(double value)
	{
		return value <= 0 ? "" : String.format(Locale.US, "%.2f", value);
	}

	private void addSearchResult()
	{
		if (searchResult != null)
		{
			content.add(searchResultHeader());
			content.add(recommendationCard(searchResult, true));
			content.add(sectionBreak());
		}
		else if (searchMessage != null && !searchMessage.trim().isEmpty())
		{
			content.add(searchResultHeader());
			content.add(paddedText(searchMessage, searchMessageColor, BODY_FONT, TEXT_WIDTH, new Insets(0, 8, 8, 8)));
			content.add(sectionBreak());
		}
	}

	private boolean hasActiveSearch()
	{
		return searchResult != null || (searchMessage != null && !searchMessage.trim().isEmpty());
	}

	private JButton clearSearchButton(String tooltip)
	{
		JButton button = new CloseButton();
		button.setToolTipText(tooltip);
		button.addActionListener(event -> clearSearch());
		return button;
	}

	private void clearSearch()
	{
		searchPopup.setVisible(false);
		selectingSearchSuggestion = true;
		searchField.setText("");
		selectingSearchSuggestion = false;
		searchResult = null;
		searchMessage = null;
		setSearchInlineMessage(null, Color.LIGHT_GRAY);
		renderCurrent();
		SwingUtilities.invokeLater(searchField::requestFocusInWindow);
	}

	private void submitSearch()
	{
		searchPopup.setVisible(false);
		String query = searchField.getText() == null ? "" : searchField.getText().trim();
		if (query.isEmpty())
		{
			searchResult = null;
			searchMessage = null;
			setSearchInlineMessage("Enter an item name or item ID.", riskColor("BALANCED"));
			renderCurrent();
			return;
		}
		setSearchInlineMessage(null, Color.LIGHT_GRAY);
		searchCallback.accept(query);
	}

	private void searchTextChanged()
	{
		if (!selectingSearchSuggestion)
		{
			if (hasSearchInlineMessage()
				&& searchField.getText() != null
				&& !searchField.getText().trim().isEmpty())
			{
				setSearchInlineMessage(null, Color.LIGHT_GRAY);
			}
			SwingUtilities.invokeLater(this::updateSearchSuggestions);
		}
	}

	private boolean hasSearchInlineMessage()
	{
		return searchInlineMessage.getText() != null && !searchInlineMessage.getText().trim().isEmpty();
	}

	private void setSearchInlineMessage(String message, Color color)
	{
		searchInlineMessage.setText(message == null || message.trim().isEmpty() ? " " : message);
		searchInlineMessage.setForeground(color);
		searchInlineMessage.repaint();
	}

	private void updateSearchSuggestions()
	{
		if (selectingSearchSuggestion || !searchField.isShowing())
		{
			return;
		}
		String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
		if (query.length() < SEARCH_MIN_QUERY_LENGTH || searchItems.isEmpty())
		{
			searchPopup.setVisible(false);
			return;
		}

		List<ItemSearchResult> suggestions = searchSuggestions(query);
		if (suggestions.isEmpty())
		{
			searchPopup.setVisible(false);
			return;
		}

		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		list.setFocusable(false);
		list.setPreferredSize(new Dimension(CONTROL_WIDTH, suggestions.size() * CONTROL_HEIGHT));
		for (ItemSearchResult item : suggestions)
		{
			list.add(searchSuggestionButton(item));
		}

		JScrollPane suggestionsScroll = new JScrollPane(list);
		suggestionsScroll.setBorder(BorderFactory.createEmptyBorder());
		suggestionsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		suggestionsScroll.getVerticalScrollBar().setUnitIncrement(16);
		suggestionsScroll.setFocusable(false);
		suggestionsScroll.setRequestFocusEnabled(false);
		suggestionsScroll.getViewport().setFocusable(false);
		int height = Math.min(SEARCH_SUGGESTION_HEIGHT, Math.max(28, suggestions.size() * 24));
		fixedSize(suggestionsScroll, CONTROL_WIDTH, height);

		searchPopup.setVisible(false);
		searchPopup.removeAll();
		searchPopup.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		searchPopup.add(suggestionsScroll);
		searchPopup.setPreferredSize(new Dimension(CONTROL_WIDTH, height));
		searchPopup.revalidate();
		searchPopup.repaint();
		searchPopup.show(searchField, 0, searchField.getHeight());
		searchPopup.pack();
		SwingUtilities.invokeLater(searchField::requestFocusInWindow);
	}

	private List<ItemSearchResult> searchSuggestions(String query)
	{
		List<ItemSearchResult> prefixMatches = new ArrayList<>();
		List<ItemSearchResult> containsMatches = new ArrayList<>();
		for (ItemSearchResult item : searchItems)
		{
			String name = item.getItemName() == null ? "" : item.getItemName();
			String lower = name.toLowerCase(Locale.ROOT);
			if (lower.startsWith(query))
			{
				prefixMatches.add(item);
			}
			else if (lower.contains(query))
			{
				containsMatches.add(item);
			}
		}

		List<ItemSearchResult> result = new ArrayList<>();
		LinkedHashSet<Integer> seen = new LinkedHashSet<>();
		addSuggestions(result, seen, prefixMatches);
		addSuggestions(result, seen, containsMatches);
		return result;
	}

	private void addSuggestions(List<ItemSearchResult> result, Set<Integer> seen, List<ItemSearchResult> matches)
	{
		for (ItemSearchResult item : matches)
		{
			if (result.size() >= SEARCH_SUGGESTION_LIMIT)
			{
				return;
			}
			if (seen.add(item.getItemId()))
			{
				result.add(item);
			}
		}
	}

	private JButton searchSuggestionButton(ItemSearchResult item)
	{
		JButton button = new JButton(item.getItemName());
		button.setToolTipText(item.getItemName() + " (" + item.getItemId() + ")");
		button.setFont(BODY_FONT);
		button.setHorizontalAlignment(JButton.LEFT);
		button.setFocusPainted(false);
		button.setFocusable(false);
		button.setRequestFocusEnabled(false);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(Color.WHITE);
		button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		fixedSize(button, CONTROL_WIDTH, CONTROL_HEIGHT);
		button.addActionListener(event -> {
			selectingSearchSuggestion = true;
			searchPopup.setVisible(false);
			searchField.setText(item.getItemName());
			selectingSearchSuggestion = false;
			searchCallback.accept(item.getItemName());
		});
		return button;
	}

	private JPanel stapleControls(int total, int maxPage)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createEmptyBorder(3, 10, 7, 10));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, STAPLE_CONTROLS_HEIGHT);

		JComboBox<String> type = new JComboBox<>(STAPLE_TYPES);
		type.setSelectedIndex(stapleTypeIndex);
		stylePinnedCombo(type, "Item category");
		fixedSize(type, CONTROL_WIDTH, CONTROL_HEIGHT);
		type.setAlignmentX(Component.CENTER_ALIGNMENT);
		deferRendersWhileOpen(type);
		type.addActionListener(event -> {
			stapleTypeIndex = type.getSelectedIndex();
			staplesPage = 0;
			showRecommendations(lastResponse);
		});

		JComboBox<String> sort = new JComboBox<>(MARKET_SORTS);
		sort.setSelectedIndex(stapleSortIndex);
		stylePinnedCombo(sort, "Sort items by");
		fixedSize(sort, CONTROL_WIDTH, CONTROL_HEIGHT);
		sort.setAlignmentX(Component.CENTER_ALIGNMENT);
		deferRendersWhileOpen(sort);
		sort.addActionListener(event -> {
			stapleSortIndex = sort.getSelectedIndex();
			staplesPage = 0;
			showRecommendations(lastResponse);
		});

		JButton previous = new PageArrowButton(false);
		previous.setEnabled(staplesPage > 0);
		previous.addActionListener(event -> {
			int updatedPage = Math.max(0, staplesPage - 1);
			if (updatedPage != staplesPage)
			{
				staplesPage = updatedPage;
				renderCurrentAtTop();
			}
		});
		JButton next = new PageArrowButton(true);
		next.setEnabled(staplesPage < maxPage);
		next.addActionListener(event -> {
			int updatedPage = Math.min(maxPage, staplesPage + 1);
			if (updatedPage != staplesPage)
			{
				staplesPage = updatedPage;
				renderCurrentAtTop();
			}
		});
		JLabel page = new JLabel((staplesPage + 1) + "/" + (maxPage + 1) + " (" + total + ")");
		page.setForeground(Color.LIGHT_GRAY);
		page.setFont(BODY_FONT);
		page.setHorizontalAlignment(JLabel.CENTER);

		JPanel pageRow = centeredControlRow();
		pageRow.add(previous, BorderLayout.WEST);
		pageRow.add(page, BorderLayout.CENTER);
		pageRow.add(next, BorderLayout.EAST);
		panel.add(type);
		panel.add(Box.createVerticalStrut(4));
		panel.add(sort);
		panel.add(Box.createVerticalStrut(4));
		panel.add(pageRow);
		return panel;
	}

	private JPanel filterControls()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createEmptyBorder(1, 10, 5, 10));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, FILTER_CONTROLS_HEIGHT);

		JPanel inner = new JPanel();
		inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
		inner.setOpaque(false);
		inner.setAlignmentX(Component.CENTER_ALIGNMENT);
		fixedSize(inner, CONTROL_WIDTH, FILTER_CONTROLS_HEIGHT - 6);

		inner.add(filterCheckbox("Stable prices only", stablePricesOnly, selected -> {
			stablePricesOnly = selected;
			resetScreenerTrendProgress();
			if (!selected && screenerTrendCancelCallback != null)
			{
				screenerTrendCancelCallback.run();
			}
			resetMarketPages();
			showRecommendations(lastResponse);
		}));
		inner.add(Box.createVerticalStrut(3));
		inner.add(filterCheckbox("Up to date prices only", upToDatePricesOnly, selected -> {
			upToDatePricesOnly = selected;
			resetScreenerTrendProgress();
			resetMarketPages();
			showRecommendations(lastResponse);
		}));

		panel.add(inner, BorderLayout.CENTER);
		return panel;
	}

	private JCheckBox filterCheckbox(String text, boolean selected, Consumer<Boolean> onChange)
	{
		JCheckBox checkbox = new JCheckBox(text);
		checkbox.setSelected(selected);
		checkbox.setFont(BODY_FONT);
		checkbox.setForeground(Color.LIGHT_GRAY);
		checkbox.setOpaque(false);
		checkbox.setFocusPainted(false);
		checkbox.setBorder(BorderFactory.createEmptyBorder());
		checkbox.setMargin(new Insets(0, 0, 0, 0));
		checkbox.setAlignmentX(LEFT_ALIGNMENT);
		checkbox.setVerticalAlignment(JCheckBox.CENTER);
		checkbox.setIcon(new FilterCheckIcon(false));
		checkbox.setRolloverIcon(new FilterCheckIcon(false));
		checkbox.setSelectedIcon(new FilterCheckIcon(true));
		checkbox.setRolloverSelectedIcon(new FilterCheckIcon(true));
		fixedSize(checkbox, CONTROL_WIDTH, 22);
		checkbox.addActionListener(event -> onChange.accept(checkbox.isSelected()));
		return checkbox;
	}

	private void stylePinnedCombo(JComboBox<String> comboBox, String tooltip)
	{
		comboBox.setFont(BODY_FONT);
		comboBox.setForeground(Color.WHITE);
		comboBox.setBackground(INPUT_SURFACE);
		comboBox.setToolTipText(tooltip);
	}

	private JPanel paginationControls(int total, int maxPage, int pageIndex, Runnable previousAction, Runnable nextAction)
	{
		return paginationControls(total, maxPage, pageIndex, false, previousAction, nextAction);
	}

	private JPanel paginationControls(int total, int maxPage, int pageIndex, boolean incomplete, Runnable previousAction, Runnable nextAction)
	{
		JButton previous = new PageArrowButton(false);
		previous.setEnabled(pageIndex > 0);
		previous.addActionListener(event -> previousAction.run());

		JButton next = new PageArrowButton(true);
		next.setEnabled(pageIndex < maxPage);
		next.addActionListener(event -> nextAction.run());

		String count = incomplete ? total + " so far" : Integer.toString(total);
		JLabel page = new JLabel((pageIndex + 1) + "/" + (maxPage + 1) + " (" + count + ")");
		screenerPaginationLabel = page;
		page.setForeground(Color.LIGHT_GRAY);
		page.setFont(BODY_FONT);
		page.setHorizontalAlignment(JLabel.CENTER);

		JPanel inner = centeredControlRow();
		inner.add(previous, BorderLayout.WEST);
		inner.add(page, BorderLayout.CENTER);
		inner.add(next, BorderLayout.EAST);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 7, 10));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, PAGE_ROW_HEIGHT);
		panel.add(inner, BorderLayout.CENTER);
		return panel;
	}

	private List<RecommendationDto> filteredAndSortedStaples(List<RecommendationDto> recommendations)
	{
		String selected = STAPLE_TYPES[stapleTypeIndex];
		List<RecommendationDto> filtered = new ArrayList<>();
		if ("All".equals(selected))
		{
			filtered.addAll(recommendations);
		}
		else
		{
			for (RecommendationDto recommendation : recommendations)
			{
				if (selected.equals(stapleType(recommendation.getItemName())))
				{
					filtered.add(recommendation);
				}
			}
		}
		filtered.sort(stapleComparator());
		return filtered;
	}

	private List<RecommendationDto> applyRecommendationFilters(List<RecommendationDto> recommendations)
	{
		if (!stablePricesOnly && !upToDatePricesOnly)
		{
			return new ArrayList<>(recommendations);
		}
		List<RecommendationDto> filtered = new ArrayList<>();
		for (RecommendationDto recommendation : recommendations)
		{
			if (stablePricesOnly && !isStableTrend(recommendation))
			{
				continue;
			}
			if (upToDatePricesOnly && recommendation.getLatestAgeMinutes() > OUTDATED_PRICE_MINUTES)
			{
				continue;
			}
			filtered.add(recommendation);
		}
		return filtered;
	}

	private List<RecommendationDto> screenerBaseFilterCandidates(List<RecommendationDto> recommendations)
	{
		List<RecommendationDto> filtered = new ArrayList<>();
		for (RecommendationDto recommendation : recommendations)
		{
			if (!passesScreenerBaseFilters(recommendation))
			{
				continue;
			}
			filtered.add(recommendation);
		}
		filtered.sort(marketComparator(screenerSortIndex));
		return filtered;
	}

	private List<RecommendationDto> applyStableScreenerFilter(List<RecommendationDto> recommendations)
	{
		if (!stablePricesOnly)
		{
			return new ArrayList<>(recommendations);
		}
		List<RecommendationDto> filtered = new ArrayList<>();
		for (RecommendationDto recommendation : recommendations)
		{
			if (isStableTrend(recommendation))
			{
				filtered.add(recommendation);
			}
		}
		return filtered;
	}

	private boolean passesScreenerBaseFilters(RecommendationDto recommendation)
	{
		if (recommendation.getNetMargin() <= 0)
		{
			return false;
		}
		if (upToDatePricesOnly && recommendation.getLatestAgeMinutes() > OUTDATED_PRICE_MINUTES)
		{
			return false;
		}
		if (screenerMinPrice > 0 && recommendation.getBuyPrice() < screenerMinPrice)
		{
			return false;
		}
		if (screenerMaxPrice > 0 && recommendation.getBuyPrice() > screenerMaxPrice)
		{
			return false;
		}
		if (screenerMinBuyVolume > 0 && recommendation.getBuyVolumePerHour() < screenerMinBuyVolume)
		{
			return false;
		}
		if (screenerMinSellVolume > 0 && recommendation.getSellVolumePerHour() < screenerMinSellVolume)
		{
			return false;
		}
		return screenerBuySellRatio <= 0 || buySellRatio(recommendation) >= screenerBuySellRatio;
	}

	private double buySellRatio(RecommendationDto recommendation)
	{
		int buyVolume = recommendation.getBuyVolumePerHour();
		int sellVolume = recommendation.getSellVolumePerHour();
		if (sellVolume <= 0)
		{
			return buyVolume > 0 ? Double.POSITIVE_INFINITY : 0;
		}
		return buyVolume / (double) sellVolume;
	}

	private String buySellRatioText(RecommendationDto recommendation)
	{
		double ratio = buySellRatio(recommendation);
		if (Double.isInfinite(ratio))
		{
			return ">99.99";
		}
		return String.format(Locale.US, "%.2f", ratio);
	}

	private boolean isStableTrend(RecommendationDto recommendation)
	{
		return "STABLE".equals(recommendation.getMarketState()) || "RANGE_BOUND".equals(recommendation.getMarketState());
	}

	private void resetMarketPages()
	{
		favoritesPage = 0;
		staplesPage = 0;
		broaderPage = 0;
	}

	private Comparator<RecommendationDto> stapleComparator()
	{
		return marketComparator(stapleSortIndex);
	}

	private Comparator<RecommendationDto> marketComparator(int sortIndex)
	{
		String selected = MARKET_SORTS[sortIndex];
		if ("ROI".equals(selected))
		{
			return Comparator
				.comparingDouble(RecommendationDto::getRoi).reversed()
				.thenComparing(Comparator.comparingLong(this::limitProfit).reversed());
		}
		if ("Buy price".equals(selected))
		{
			return Comparator
				.comparingInt(RecommendationDto::getBuyPrice)
				.thenComparing(Comparator.comparingDouble(RecommendationDto::getRoi).reversed());
		}
		if ("Net margin".equals(selected))
		{
			return Comparator
				.comparingInt(RecommendationDto::getNetMargin).reversed()
				.thenComparing(Comparator.comparingLong(this::limitProfit).reversed());
		}
		if ("Vol/hr".equals(selected))
		{
			return Comparator
				.comparingInt(RecommendationDto::getHourlyVolume).reversed()
				.thenComparing(Comparator.comparingLong(this::limitProfit).reversed());
		}
		if ("Limit P&L".equals(selected))
		{
			return Comparator
				.comparingLong(this::limitProfit).reversed()
				.thenComparing(Comparator.comparingDouble(RecommendationDto::getRoi).reversed());
		}
		return Comparator
			.comparingLong(this::flowProfit).reversed()
			.thenComparing(Comparator.comparingLong(this::limitProfit).reversed())
			.thenComparing(Comparator.comparingDouble(RecommendationDto::getRoi).reversed());
	}

	private long flowProfit(RecommendationDto rec)
	{
		return rec.getFourHourFlowProfit();
	}

	private long limitProfit(RecommendationDto rec)
	{
		return (long) rec.getNetMargin() * rec.getBuyLimit();
	}

	private String stapleType(String name)
	{
		return HighVolumeItemCatalog.category(name);
	}

	private JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		label.setFont(SECTION_FONT);
		label.setBorder(BorderFactory.createEmptyBorder(12, 8, 6, 8));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private JComponent sectionHelp(String text)
	{
		return paddedText(text, Color.LIGHT_GRAY, BODY_FONT, TEXT_WIDTH, new Insets(0, 8, 6, 8));
	}

	private JComponent searchResultHeader()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(new Color(34, 34, 34));
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(55, 55, 55)),
			BorderFactory.createEmptyBorder(7, 8, 7, 8)
		));
		panel.setAlignmentX(LEFT_ALIGNMENT);

		JLabel label = new JLabel("Item Lookup Result");
		label.setForeground(Color.WHITE);
		label.setFont(SECTION_FONT);
		panel.add(label, BorderLayout.WEST);
		panel.add(clearSearchButton("Close item lookup result"), BorderLayout.EAST);

		Dimension preferred = panel.getPreferredSize();
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
		return panel;
	}

	private JComponent sectionBreak()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panel.setAlignmentX(LEFT_ALIGNMENT);

		JPanel line = new JPanel();
		line.setBackground(new Color(48, 48, 48));
		line.setPreferredSize(new Dimension(TEXT_WIDTH, 1));
		panel.add(line, BorderLayout.CENTER);

		Dimension preferred = panel.getPreferredSize();
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
		return panel;
	}

	private JComponent pinnedHeading(String titleText, String description)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(true);
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createEmptyBorder(7, 10, 5, 10));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, PINNED_HELP_HEIGHT);
		JLabel title = new JLabel(titleText);
		title.setForeground(Color.WHITE);
		title.setFont(SECTION_FONT);
		title.setAlignmentX(CENTER_ALIGNMENT);
		JLabel help = new JLabel(description);
		help.setForeground(Color.LIGHT_GRAY);
		help.setFont(STAT_LABEL_FONT);
		help.setAlignmentX(CENTER_ALIGNMENT);
		panel.add(title);
		panel.add(Box.createVerticalStrut(2));
		panel.add(help);
		return panel;
	}

	private JPanel marketNav()
	{
		JPanel panel = new JPanel(new GridLayout(1, 4, 6, 0));
		panel.setBackground(new Color(27, 27, 27));
		panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, NAV_HEIGHT);
		panel.add(navButton("favorites", "Favorites", favoritesExpanded && !controlsCollapsed, favoritesExpanded && !controlsCollapsed, () -> selectMarketSection("favorites")));
		panel.add(navButton("highVolume", "High-volume items", highVolumeExpanded && !controlsCollapsed, highVolumeExpanded && !controlsCollapsed, () -> selectMarketSection("highVolume")));
		panel.add(navButton("screener", "Screener", broaderExpanded && !controlsCollapsed, broaderExpanded && !controlsCollapsed, () -> selectMarketSection("broader")));
		panel.add(navButton("flipLog", "Flip Log", flipLogExpanded && !controlsCollapsed, flipLogExpanded && !controlsCollapsed, () -> selectMarketSection("flipLog")));
		return panel;
	}

	private JPanel utilityNav()
	{
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
		panel.setBackground(PINNED_SURFACE);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(47, 47, 47)),
			BorderFactory.createEmptyBorder(0, 10, 0, 10)
		));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedHeight(panel, NAV_HEIGHT);
		panel.add(utilityButton("calculator", "Profit calculator", calculatorExpanded, this::toggleCalculator));
		return panel;
	}

	private JButton navButton(String icon, String tooltip, boolean selected, boolean expanded, Runnable action)
	{
		JButton button = new MarketNavButton(icon, selected, expanded);
		button.setToolTipText(tooltip + (expanded ? " (hide controls)" : " (show controls)"));
		button.addActionListener(event -> action.run());
		return button;
	}

	private JButton utilityButton(String icon, String tooltip, boolean expanded, Runnable action)
	{
		JButton button = new MarketNavButton(icon, expanded, expanded);
		fixedButtonSize(button, 44, 30);
		button.setToolTipText((expanded ? "Hide " : "Show ") + tooltip.toLowerCase(Locale.US) + ".");
		button.addActionListener(event -> action.run());
		return button;
	}

	private void selectMarketSection(String section)
	{
		boolean currentlySelected = ("favorites".equals(section) && favoritesExpanded)
			|| ("highVolume".equals(section) && highVolumeExpanded)
			|| ("broader".equals(section) && broaderExpanded)
			|| ("flipLog".equals(section) && flipLogExpanded);
		if (currentlySelected)
		{
			setControlsCollapsed(!controlsCollapsed);
			return;
		}
		favoritesExpanded = "favorites".equals(section);
		highVolumeExpanded = "highVolume".equals(section);
		broaderExpanded = "broader".equals(section);
		flipLogExpanded = "flipLog".equals(section);
		if (controlsCollapsed)
		{
			controlsCollapsed = false;
			if (controlsCollapsedCallback != null)
			{
				controlsCollapsedCallback.accept(false);
			}
		}
		renderCurrentAtTop();
	}

	private List<FlipRecord> filteredFlipLogRecords()
	{
		long cutoff = selectedFlipLogCutoff();
		String query = flipLogSearchField.getText() == null
			? ""
			: flipLogSearchField.getText().trim().toLowerCase(Locale.US);
		List<FlipRecord> filtered = new ArrayList<>();
		for (FlipRecord record : flipLogRecords)
		{
			if (record == null || record.getCompletedAt() < cutoff)
			{
				continue;
			}
			String itemName = record.getItemName() == null ? "" : record.getItemName().toLowerCase(Locale.US);
			if (!query.isEmpty() && !itemName.contains(query))
			{
				continue;
			}
			filtered.add(record);
		}
		return filtered;
	}

	private long selectedFlipLogCutoff()
	{
		long now = Instant.now().getEpochSecond();
		switch (flipLogTimeframeIndex)
		{
			case 0:
				return panelStartedAt;
			case 1:
				return now - 60L * 60L;
			case 2:
				return now - 4L * 60L * 60L;
			case 3:
				return now - 12L * 60L * 60L;
			case 4:
				return now - 24L * 60L * 60L;
			case 5:
				return now - 7L * 24L * 60L * 60L;
			case 6:
				return now - 30L * 24L * 60L * 60L;
			default:
				return Long.MIN_VALUE;
		}
	}

	private List<FlipItemTotal> summarizedFlipItems(List<FlipRecord> records)
	{
		Map<Integer, FlipItemTotal> byItem = new LinkedHashMap<>();
		for (FlipRecord record : records)
		{
			FlipItemTotal total = byItem.computeIfAbsent(
				record.getItemId(),
				itemId -> new FlipItemTotal(itemId, record.getItemName())
			);
			total.add(record);
		}
		List<FlipItemTotal> items = new ArrayList<>(byItem.values());
		if (flipLogSortIndex == 1)
		{
			items.sort(Comparator.comparingLong(FlipItemTotal::getProfit).reversed()
				.thenComparing(Comparator.comparingLong(FlipItemTotal::getLastCompletedAt).reversed()));
		}
		else
		{
			items.sort(Comparator.comparingLong(FlipItemTotal::getLastCompletedAt).reversed()
				.thenComparing(Comparator.comparingLong(FlipItemTotal::getProfit).reversed()));
		}
		return items;
	}

	private JPanel flipLogSummaryCard(List<FlipRecord> records)
	{
		long profit = 0;
		long cost = 0;
		long quantity = 0;
		Set<String> saleOfferKeys = new HashSet<>();
		for (FlipRecord record : records)
		{
			profit += record.getProfit();
			cost += record.getCost();
			quantity += record.getQuantity();
			saleOfferKeys.add(record.getSaleOfferKey() == null
				? record.getItemId() + ":" + record.getCompletedAt()
				: record.getSaleOfferKey());
		}
		JPanel card = baseCard(true);
		JLabel label = new JLabel("Total Profit");
		label.setForeground(TAB_ACCENT);
		label.setFont(TITLE_FONT);
		label.setAlignmentX(LEFT_ALIGNMENT);
		card.add(label);
		JLabel value = new JLabel(signedGp(profit));
		value.setForeground(profit >= 0 ? riskColor("SAFE") : riskColor("ADVANCED"));
		value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
		value.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
		value.setAlignmentX(LEFT_ALIGNMENT);
		card.add(value);
		card.add(metricRow("Net ROI", cost <= 0 ? "-" : String.format(Locale.US, "%.2f%%", profit * 100.0 / cost), Color.WHITE));
		card.add(metricRow("Flips", GP.format(saleOfferKeys.size()), Color.WHITE));
		card.add(metricRow("Quantity", GP.format(quantity), Color.WHITE));
		return finish(card);
	}

	private JPanel flipItemCard(FlipItemTotal item)
	{
		JPanel card = baseCard(false);
		JPanel heading = new JPanel(new BorderLayout(6, 0));
		heading.setOpaque(false);
		heading.setAlignmentX(LEFT_ALIGNMENT);
		heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, ITEM_ICON_SIZE + 4));
		heading.add(itemIcon(item.getItemId()), BorderLayout.WEST);
		heading.add(wrapLabel(item.getItemName(), Color.WHITE, TITLE_FONT, TEXT_WIDTH - ITEM_ICON_SIZE - 8), BorderLayout.CENTER);
		card.add(heading);
		card.add(Box.createVerticalStrut(6));
		card.add(metricRow("Profit", signedGp(item.getProfit()), item.getProfit() >= 0 ? riskColor("SAFE") : riskColor("ADVANCED")));
		card.add(metricRow("Net ROI", item.getCost() <= 0 ? "-" : String.format(Locale.US, "%.2f%%", item.getProfit() * 100.0 / item.getCost()), Color.WHITE));
		card.add(metricRow("Flips", GP.format(item.getFlipCount()), Color.WHITE));
		card.add(metricRow("Quantity", GP.format(item.getQuantity()), Color.WHITE));
		card.add(metricRow("Last flip", updatedText((int) Math.max(0, (Instant.now().getEpochSecond() - item.getLastCompletedAt()) / 60)), Color.LIGHT_GRAY));
		return finish(card);
	}

	private JPanel recommendationCard(RecommendationDto rec)
	{
		return recommendationCard(rec, false);
	}

	private JPanel recommendationCard(RecommendationDto rec, boolean highlighted)
	{
		JPanel card = baseCard(highlighted);
		card.add(recommendationHeader(rec));
		card.add(statusPills(rec));
		card.add(metricRow("Buy at", gp(rec.getBuyPrice()), Color.WHITE));
		card.add(metricRow("Sell at", gp(rec.getSellPrice()), Color.WHITE));
		card.add(metricRow(
			"Net margin",
			signedGp(rec.getNetMargin()),
			rec.getNetMargin() >= 0 ? riskColor("SAFE") : riskColor("ADVANCED"),
			netMarginTooltip(rec)
		));
		card.add(metricRow("ROI", String.format("%.2f%%", rec.getRoi() * 100), Color.WHITE));
		card.add(metricRow(
			"Limit P&L",
			signedGp(limitProfit(rec)),
			rec.getNetMargin() >= 0 ? riskColor("SAFE") : riskColor("ADVANCED"),
			limitPnlTooltip(rec)
		));
		card.add(metricRow(
			"Flow P&L",
			signedGp(flowProfit(rec)),
			flowProfit(rec) >= 0 ? riskColor("SAFE") : riskColor("ADVANCED"),
			flowPnlTooltip(rec)
		));
		if (rec.isSeverelyFlowLimited())
		{
			card.add(liquidityWarning(rec));
		}
		card.add(cardSeparator());
		card.add(metricRow("Buy vol/hr", GP.format(rec.getBuyVolumePerHour()), Color.LIGHT_GRAY));
		card.add(metricRow("Sell vol/hr", GP.format(rec.getSellVolumePerHour()), Color.LIGHT_GRAY));
		card.add(metricRow("B/S ratio", buySellRatioText(rec), Color.LIGHT_GRAY));
		card.add(metricRow("Buy limit", GP.format(rec.getBuyLimit()) + " / 4h", Color.LIGHT_GRAY));
		card.add(metricRow("Last update", updatedText(rec.getLatestAgeMinutes()), ageColor(rec.getLatestAgeMinutes())));
		return finish(card);
	}

	private JPanel recommendationHeader(RecommendationDto rec)
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
		header.setOpaque(false);
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		header.setAlignmentX(LEFT_ALIGNMENT);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		JLabel icon = itemIcon(rec.getItemId());
		JComponent label = wrapLabel(rec.getItemName(), Color.WHITE, TITLE_FONT, TITLE_TEXT_WIDTH);
		label.setAlignmentY(Component.TOP_ALIGNMENT);
		JButton favorite = heartButton(rec);
		JButton chart = chartButton(rec);
		JPanel actions = new JPanel();
		actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
		actions.setOpaque(false);
		actions.setAlignmentY(Component.TOP_ALIGNMENT);
		actions.add(favorite);
		actions.add(Box.createVerticalStrut(2));
		actions.add(chart);
		header.add(icon);
		header.add(Box.createHorizontalStrut(6));
		header.add(label);
		header.add(Box.createHorizontalGlue());
		header.add(actions);
		Dimension preferred = header.getPreferredSize();
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
		return header;
	}

	private JLabel itemIcon(int itemId)
	{
		JLabel label = new JLabel();
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
		label.setOpaque(true);
		label.setBackground(new Color(28, 28, 28));
		label.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50)));
		label.setAlignmentY(Component.TOP_ALIGNMENT);
		fixedSize(label, ITEM_ICON_SIZE, ITEM_ICON_SIZE);
		if (itemManager != null)
		{
			try
			{
				AsyncBufferedImage image = itemManager.getImage(itemId);
				if (image != null)
				{
					setCenteredItemIcon(label, image);
					image.onLoaded(() -> SwingUtilities.invokeLater(() -> setCenteredItemIcon(label, image)));
				}
			}
			catch (RuntimeException ignored)
			{
				// Leave the placeholder visible if RuneLite cannot provide an item sprite yet.
			}
		}
		return label;
	}

	private void setCenteredItemIcon(JLabel label, BufferedImage image)
	{
		label.setIcon(new ImageIcon(centeredItemImage(image)));
		label.repaint();
	}

	private BufferedImage centeredItemImage(BufferedImage source)
	{
		int minX = source.getWidth();
		int minY = source.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				if (((source.getRGB(x, y) >>> 24) & 0xff) > 8)
				{
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		int canvasSize = ITEM_ICON_SIZE - 4;
		BufferedImage centered = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
		if (maxX < minX || maxY < minY)
		{
			return centered;
		}

		int sourceWidth = maxX - minX + 1;
		int sourceHeight = maxY - minY + 1;
		double scale = Math.min(1.0, Math.min(canvasSize / (double) sourceWidth, canvasSize / (double) sourceHeight));
		int drawWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
		int drawHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
		int drawX = (canvasSize - drawWidth) / 2;
		int drawY = (canvasSize - drawHeight) / 2;

		Graphics2D graphics = centered.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(source, drawX, drawY, drawX + drawWidth, drawY + drawHeight, minX, minY, maxX + 1, maxY + 1, null);
		graphics.dispose();
		return centered;
	}

	private JButton heartButton(RecommendationDto rec)
	{
		boolean favorite = favoriteItemIds.contains(rec.getItemId());
		JButton button = new HeartButton(favorite);
		button.setToolTipText(favorite ? "Unfavorite" : "Favorite");
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.addActionListener(event -> favoriteCallback.accept(rec.getItemId()));
		return button;
	}

	private JButton chartButton(RecommendationDto rec)
	{
		JButton button = new ChartButton();
		button.setToolTipText("Daily chart");
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.addActionListener(event -> {
			if (chartCallback != null)
			{
				chartCallback.accept(rec, ChartPeriod.DAILY);
			}
		});
		return button;
	}

	private void openChartDialog(RecommendationDto recommendation, ChartPeriod period, List<TimeseriesPoint> points)
	{
		String itemName = safeItemName(recommendation.getItemName());
		Window owner = SwingUtilities.getWindowAncestor(this);
		boolean replacingItem = dailyChartFrame == null || openChartItemId != recommendation.getItemId();
		if (replacingItem && dailyChartFrame != null)
		{
			dailyChartFrame.dispose();
			dailyChartFrame = null;
		}

		JFrame frame = dailyChartFrame;
		if (frame == null)
		{
			frame = new JFrame();
			JFrame newFrame = frame;
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setResizable(true);
			frame.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosed(WindowEvent event)
				{
					if (dailyChartFrame == newFrame)
					{
						dailyChartFrame = null;
						openChartItemId = -1;
					}
				}
			});
		}
		frame.setTitle(period.label + " chart - " + itemName);

		JPanel shell = new JPanel(new BorderLayout(0, 8));
		shell.setBackground(ColorScheme.DARK_GRAY_COLOR);
		shell.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel(itemName);
		title.setForeground(Color.WHITE);
		title.setFont(SECTION_FONT);

		JPanel heading = new JPanel(new BorderLayout(8, 0));
		heading.setOpaque(false);
		heading.add(title, BorderLayout.WEST);
		heading.add(chartPeriodControls(recommendation, period), BorderLayout.EAST);

		DailyChartPanel chart = new DailyChartPanel(points, period);
		chart.setPreferredSize(new Dimension(1240, 650));

		JLabel legend = new JLabel(period.label + " " + period.intervalLabel + " Wiki history. Hover for exact interval price and volume.");
		legend.setForeground(Color.LIGHT_GRAY);
		legend.setFont(STAT_LABEL_FONT);

		shell.add(heading, BorderLayout.NORTH);
		shell.add(chart, BorderLayout.CENTER);
		shell.add(legend, BorderLayout.SOUTH);
		frame.setContentPane(shell);
		if (dailyChartFrame == null)
		{
			frame.pack();
			frame.setMinimumSize(new Dimension(680, 420));
			frame.setLocationRelativeTo(owner == null ? this : owner);
		}
		else
		{
			frame.revalidate();
			frame.repaint();
		}
		openChartItemId = recommendation.getItemId();
		dailyChartFrame = frame;
		frame.setVisible(true);
	}

	private JPanel chartPeriodControls(RecommendationDto recommendation, ChartPeriod selectedPeriod)
	{
		JPanel controls = new JPanel(new GridLayout(1, 2, 4, 0));
		controls.setOpaque(false);
		controls.add(chartPeriodButton("Daily", recommendation, ChartPeriod.DAILY, selectedPeriod));
		controls.add(chartPeriodButton("Weekly", recommendation, ChartPeriod.WEEKLY, selectedPeriod));
		return controls;
	}

	private JButton chartPeriodButton(
		String text,
		RecommendationDto recommendation,
		ChartPeriod period,
		ChartPeriod selectedPeriod)
	{
		boolean selected = period == selectedPeriod;
		JButton button = new JButton(text);
		button.setFont(BODY_FONT);
		button.setFocusPainted(false);
		button.setMargin(new Insets(0, 10, 0, 10));
		button.setForeground(selected ? Color.WHITE : Color.LIGHT_GRAY);
		button.setBackground(selected ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		button.setOpaque(true);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, selected ? 2 : 1, 0, selected ? TAB_ACCENT : ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(5, 10, 5, 10)
		));
		button.addActionListener(event -> {
			if (!selected && chartCallback != null)
			{
				chartCallback.accept(recommendation, period);
			}
		});
		return button;
	}

	private JButton favoriteButton(RecommendationDto rec)
	{
		boolean favorite = favoriteItemIds.contains(rec.getItemId());
		JButton button = new JButton(favorite ? "♥" : "♡");
		button.setToolTipText(favorite ? "Unfavorite" : "Favorite");
		button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
		button.setForeground(favorite ? TAB_ACCENT : Color.LIGHT_GRAY);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setOpaque(false);
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setPreferredSize(new Dimension(28, 24));
		button.addActionListener(event -> favoriteCallback.accept(rec.getItemId()));
		return button;
	}

	private void addTrend(JPanel card, RecommendationDto rec)
	{
		if (rec.getMarketState() == null || "UNKNOWN".equals(rec.getMarketState()))
		{
			card.add(priceRow("Trend", "Checking...", Color.LIGHT_GRAY));
			return;
		}
		card.add(priceRow("Trend", trendText(rec.getMarketState()), trendColor(rec.getMarketState())));
	}

	private JPanel baseCard()
	{
		return baseCard(false);
	}

	private JPanel baseCard(boolean highlighted)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(new Color(30, 30, 30));
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(highlighted ? TAB_ACCENT : new Color(43, 43, 43)),
			BorderFactory.createEmptyBorder(9, 8, 9, 8)
		));
		card.setAlignmentX(LEFT_ALIGNMENT);
		return card;
	}

	private JPanel metricRow(String left, String right, Color rightColor)
	{
		return metricRow(left, right, rightColor, null);
	}

	private JPanel metricRow(String left, String right, Color rightColor, String tooltip)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(row, METRIC_ROW_WIDTH, 19);

		JLabel leftLabel = new JLabel(left);
		leftLabel.setForeground(Color.GRAY);
		leftLabel.setFont(STAT_LABEL_FONT);
		leftLabel.setToolTipText(tooltip);
		fixedSize(leftLabel, METRIC_LABEL_WIDTH, 17);

		JLabel rightLabel = new JLabel(right == null ? "" : right);
		rightLabel.setForeground(rightColor);
		rightLabel.setFont(BODY_FONT);
		rightLabel.setHorizontalAlignment(JLabel.RIGHT);
		rightLabel.setToolTipText(tooltip);
		row.setToolTipText(tooltip);

		row.add(leftLabel, BorderLayout.WEST);
		row.add(rightLabel, BorderLayout.CENTER);
		return row;
	}

	private String netMarginTooltip(RecommendationDto rec)
	{
		int grossMargin = rec.getSellPrice() - rec.getBuyPrice();
		return "<html>"
			+ "Per item after GE tax.<br>"
			+ "Gross spread: " + signedGp(grossMargin) + "<br>"
			+ "Tax per item: -" + gp(rec.getTax()) + "<br>"
			+ "Net margin: " + signedGp(rec.getNetMargin())
			+ "</html>";
	}

	private String limitPnlTooltip(RecommendationDto rec)
	{
		int buyLimit = rec.getBuyLimit();
		int grossMargin = rec.getSellPrice() - rec.getBuyPrice();
		long grossProfit = (long) grossMargin * buyLimit;
		long estimatedTax = (long) rec.getTax() * buyLimit;
		long postTaxProfit = (long) rec.getNetMargin() * buyLimit;
		return "<html>"
			+ "Per item spread: " + signedGp(grossMargin) + "<br>"
			+ "Tax per item: -" + gp(rec.getTax()) + "<br>"
			+ "Net per item: " + signedGp(rec.getNetMargin()) + "<br><br>"
			+ "Full 4h limit pre-tax: " + signedGp(grossProfit) + "<br>"
			+ "Full 4h limit tax: -" + gp(estimatedTax) + "<br>"
			+ "Full 4h limit post-tax: " + signedGp(postTaxProfit)
			+ "</html>";
	}

	private String flowPnlTooltip(RecommendationDto rec)
	{
		int weakerHourlySide = Math.max(0, Math.min(rec.getBuyVolumePerHour(), rec.getSellVolumePerHour()));
		return "<html>"
			+ "Weaker-side observed volume: " + GP.format(weakerHourlySide) + " / hr<br>"
			+ "Est. 4h traded qty: " + GP.format(rec.getFourHourFlowQuantity()) + " / " + GP.format(rec.getBuyLimit()) + " limit<br>"
			+ "Flow P&L: " + signedGp(rec.getFourHourFlowProfit()) + "<br><br>"
			+ "Capped by the lower buy/sell hourly volume and the GE buy limit.<br>"
			+ "Estimated from observed volume. Fills are not guaranteed."
			+ "</html>";
	}

	private JPanel liquidityWarning(RecommendationDto rec)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(3, 0, 4, 0));
		row.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(row, TEXT_WIDTH, 25);
		row.add(statusPill(
			"Thin volume",
			riskColor("BALANCED"),
			"Est. 4h traded qty: " + GP.format(rec.getFourHourFlowQuantity()) + " of "
				+ GP.format(rec.getBuyLimit()) + " buy-limit items."
		));
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private JComponent cardSeparator()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(panel, METRIC_ROW_WIDTH, 11);

		JPanel line = new JPanel();
		line.setBackground(new Color(48, 48, 48));
		line.setPreferredSize(new Dimension(METRIC_ROW_WIDTH, 1));
		panel.add(line, BorderLayout.CENTER);
		return panel;
	}

	private JPanel statusPills(RecommendationDto rec)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
		row.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(row, TEXT_WIDTH, 24);
		row.add(statusPill(trendStatusText(rec), trendStatusColor(rec), "Based on OSRS Wiki 5-minute price history over roughly the last 24 hours."));
		if (rec.getLatestAgeMinutes() > OUTDATED_PRICE_MINUTES)
		{
			row.add(Box.createHorizontalStrut(6));
			row.add(statusPill("Outdated", riskColor("ADVANCED")));
		}
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private JLabel statusPill(String text, Color color)
	{
		return statusPill(text, color, null);
	}

	private JLabel statusPill(String text, Color color, String tooltip)
	{
		JLabel label = new JLabel(text);
		label.setFont(STAT_LABEL_FONT);
		label.setForeground(color);
		label.setToolTipText(tooltip);
		label.setOpaque(true);
		label.setBackground(new Color(24, 24, 24));
		label.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(
				Math.max(0, color.getRed() - 60),
				Math.max(0, color.getGreen() - 60),
				Math.max(0, color.getBlue() - 60)
			)),
			BorderFactory.createEmptyBorder(2, 6, 2, 6)
		));
		return label;
	}

	private JPanel priceRow(String left, String right, Color rightColor)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		row.setAlignmentX(LEFT_ALIGNMENT);
		JLabel leftLabel = new JLabel(left);
		leftLabel.setForeground(Color.GRAY);
		leftLabel.setFont(STAT_LABEL_FONT);
		leftLabel.setAlignmentX(LEFT_ALIGNMENT);
		leftLabel.setMaximumSize(new Dimension(TEXT_WIDTH, 14));
		JComponent rightLabel = wrapLabel(right, rightColor, BODY_FONT, TEXT_WIDTH);
		row.add(leftLabel);
		row.add(rightLabel);
		return row;
	}

	private JComponent title(String text)
	{
		return paddedText(text, Color.WHITE, TITLE_FONT, TEXT_WIDTH, new Insets(0, 0, 5, 0));
	}

	private JComponent wrapped(String text)
	{
		return paddedText(text, Color.LIGHT_GRAY, BODY_FONT, TEXT_WIDTH, new Insets(5, 0, 0, 0));
	}

	private JComponent paddedText(String text, Color color, Font font, int width, Insets padding)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(wrapLabel(text, color, font, width));
		Dimension preferred = panel.getPreferredSize();
		panel.setMaximumSize(new Dimension(width + padding.left + padding.right, preferred.height));
		return panel;
	}

	private JComponent wrapLabel(String text, Color color, Font font, int width)
	{
		JTextArea label = new WrappedTextArea(text == null ? "" : text);
		label.setEditable(false);
		label.setFocusable(false);
		label.setOpaque(false);
		label.setAutoscrolls(false);
		label.setLineWrap(true);
		label.setWrapStyleWord(true);
		label.setForeground(color);
		label.setFont(font);
		label.setBorder(BorderFactory.createEmptyBorder());
		label.setMargin(new Insets(0, 0, 0, 0));
		label.setAlignmentX(LEFT_ALIGNMENT);
		label.setSize(new Dimension(width, Short.MAX_VALUE));
		Dimension preferred = label.getPreferredSize();
		label.setPreferredSize(new Dimension(width, preferred.height));
		label.setMinimumSize(new Dimension(0, preferred.height));
		label.setMaximumSize(new Dimension(width, preferred.height));
		return label;
	}

	private int currentScrollValue()
	{
		return scrollPane == null ? 0 : scrollPane.getVerticalScrollBar().getValue();
	}

	private void restoreScrollPosition(int value)
	{
		if (scrollPane == null)
		{
			return;
		}
		SwingUtilities.invokeLater(() -> {
			JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
			int max = Math.max(verticalBar.getMinimum(), verticalBar.getMaximum() - verticalBar.getVisibleAmount());
			verticalBar.setValue(Math.max(verticalBar.getMinimum(), Math.min(value, max)));
		});
	}

	private void fixedSize(JComponent component, int width, int height)
	{
		Dimension size = new Dimension(width, height);
		component.setMinimumSize(size);
		component.setPreferredSize(size);
		component.setMaximumSize(size);
	}

	private void flexibleRow(JComponent component, int preferredWidth, int height)
	{
		component.setMinimumSize(new Dimension(0, height));
		component.setPreferredSize(new Dimension(preferredWidth, height));
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
	}

	private void fixedHeight(JComponent component, int height)
	{
		component.setMinimumSize(new Dimension(0, height));
		component.setPreferredSize(new Dimension(CONTROL_WIDTH + 16, height));
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
	}

	private JPanel centeredControlRow()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		fixedSize(row, CONTROL_WIDTH, CONTROL_HEIGHT);
		return row;
	}

	private void deferRendersWhileOpen(JComboBox<?> comboBox)
	{
		comboBox.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent event)
			{
				comboPopupOpen = true;
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
			{
				comboPopupOpen = false;
				flushDeferredRecommendationRender();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent event)
			{
				comboPopupOpen = false;
				flushDeferredRecommendationRender();
			}
		});
	}

	private boolean isPopupInteractionOpen()
	{
		return comboPopupOpen || searchPopup.isVisible();
	}

	private boolean isTextEditingActive()
	{
		return searchField.isFocusOwner()
			|| calculatorBuyPriceField.isFocusOwner()
			|| calculatorSellPriceField.isFocusOwner()
			|| calculatorQuantityField.isFocusOwner()
			|| flipLogSearchField.isFocusOwner()
			|| screenerMinPriceField.isFocusOwner()
			|| screenerMaxPriceField.isFocusOwner()
			|| screenerMinBuyVolumeField.isFocusOwner()
			|| screenerMinSellVolumeField.isFocusOwner()
			|| screenerBuySellRatioField.isFocusOwner();
	}

	private void flushDeferredRecommendationRender()
	{
		if (deferredRecommendationResponse == null)
		{
			return;
		}
		SwingUtilities.invokeLater(() -> {
			if (isPopupInteractionOpen() || isTextEditingActive() || deferredRecommendationResponse == null)
			{
				return;
			}
			SignalResponse response = deferredRecommendationResponse;
			int scrollValue = deferredScrollValue;
			deferredRecommendationResponse = null;
			renderRecommendations(response);
			restoreScrollPosition(scrollValue);
		});
	}

	private void bindWheelScrolling(Component component)
	{
		boolean alreadyBound = false;
		for (MouseWheelListener listener : component.getMouseWheelListeners())
		{
			if (listener == wheelListener)
			{
				alreadyBound = true;
				break;
			}
		}
		if (!alreadyBound)
		{
			component.addMouseWheelListener(wheelListener);
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				bindWheelScrolling(child);
			}
		}
	}

	private void scrollPanel(MouseWheelEvent event)
	{
		if (scrollPane == null)
		{
			return;
		}
		JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
		int increment = Math.max(16, verticalBar.getUnitIncrement(event.getWheelRotation()));
		int max = verticalBar.getMaximum() - verticalBar.getVisibleAmount();
		int target = verticalBar.getValue() + event.getUnitsToScroll() * increment;
		verticalBar.setValue(Math.max(verticalBar.getMinimum(), Math.min(target, max)));
		event.consume();
	}

	private JPanel finish(JPanel card)
	{
		Dimension preferred = card.getPreferredSize();
		card.setMinimumSize(new Dimension(METRIC_ROW_WIDTH + 16, preferred.height));
		card.setPreferredSize(new Dimension(METRIC_ROW_WIDTH + 16, preferred.height));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
		return card;
	}

	private JPanel emptyState(String title, String body)
	{
		JPanel panel = baseCard();
		panel.add(wrapped(title));
		panel.add(wrapped(body));
		return finish(panel);
	}

	private Color riskColor(String risk)
	{
		if ("SAFE".equals(risk))
		{
			return new Color(88, 204, 125);
		}
		if ("BALANCED".equals(risk))
		{
			return new Color(235, 190, 92);
		}
		return new Color(230, 115, 115);
	}

	private Color ageColor(int minutes)
	{
		if (minutes <= OUTDATED_PRICE_MINUTES)
		{
			return Color.WHITE;
		}
		return new Color(230, 115, 115);
	}

	private String updatedText(int minutes)
	{
		return age(minutes);
	}

	private String updatedStatus(long generatedAt)
	{
		long now = System.currentTimeMillis() / 1000L;
		int ageMinutes = (int) Math.max(0, (now - generatedAt) / 60L);
		return "Last updated " + TIME.format(Instant.ofEpochSecond(generatedAt)).toLowerCase(Locale.US) + " (" + age(ageMinutes) + ")";
	}

	private void restoreFooterStatus()
	{
		if (lastResponse != null)
		{
			status.setText(updatedStatus(lastResponse.getGeneratedAt()));
		}
	}

	private String trendStatusText(RecommendationDto rec)
	{
		if (rec.getMarketState() == null || "UNKNOWN".equals(rec.getMarketState()))
		{
			return "Checking";
		}
		return trendText(rec.getMarketState());
	}

	private Color trendStatusColor(RecommendationDto rec)
	{
		if (rec.getMarketState() == null || "UNKNOWN".equals(rec.getMarketState()))
		{
			return Color.LIGHT_GRAY;
		}
		return trendColor(rec.getMarketState());
	}

	private Color trendColor(String state)
	{
		String trend = trendText(state);
		if ("Stable".equals(trend))
		{
			return new Color(88, 204, 125);
		}
		if ("Rising".equals(trend))
		{
			return new Color(235, 190, 92);
		}
		return "Falling".equals(trend) ? new Color(230, 115, 115) : Color.LIGHT_GRAY;
	}

	private String trendText(String state)
	{
		if ("STABLE".equals(state) || "RANGE_BOUND".equals(state))
		{
			return "Stable";
		}
		if ("RISING".equals(state) || "SPIKING".equals(state))
		{
			return "Rising";
		}
		if ("FALLING".equals(state) || "CRASHING".equals(state))
		{
			return "Falling";
		}
		return "Unknown trend";
	}

	private String gp(int value)
	{
		return GP.format(value) + " gp";
	}

	private String gp(long value)
	{
		return GP.format(value) + " gp";
	}

	private String safeItemName(String itemName)
	{
		return itemName == null || itemName.trim().isEmpty() ? "item" : itemName;
	}

	private String age(int minutes)
	{
		if (minutes == Integer.MAX_VALUE)
		{
			return "unknown";
		}
		if (minutes < 60)
		{
			return minutes + " mins ago";
		}
		int hours = minutes / 60;
		if (hours == 1)
		{
			return "1 hr ago";
		}
		return hours + " hrs ago";
	}

	private String signedGp(int value)
	{
		return (value >= 0 ? "+" : "") + gp(value);
	}

	private String signedGp(long value)
	{
		return (value >= 0 ? "+" : "") + gp(value);
	}

	private class WheelScrollPane extends JScrollPane
	{
		WheelScrollPane(Component view)
		{
			super(view);
			setWheelScrollingEnabled(true);
		}

		@Override
		protected void processMouseWheelEvent(MouseWheelEvent event)
		{
			scrollPanel(event);
		}
	}

	private static class WrappedTextArea extends JTextArea
	{
		private WrappedTextArea(String text)
		{
			super(text);
		}

		@Override
		public void scrollRectToVisible(Rectangle rect)
		{
			// Text labels must never move the plugin panel's viewport during re-render.
		}
	}

	private static class FlipItemTotal
	{
		private final int itemId;
		private final String itemName;
		private long quantity;
		private long cost;
		private long profit;
		private long lastCompletedAt;
		private final Set<String> saleOfferKeys = new HashSet<>();

		private FlipItemTotal(int itemId, String itemName)
		{
			this.itemId = itemId;
			this.itemName = itemName == null || itemName.trim().isEmpty() ? "Item " + itemId : itemName;
		}

		private void add(FlipRecord record)
		{
			quantity += record.getQuantity();
			cost += record.getCost();
			profit += record.getProfit();
			lastCompletedAt = Math.max(lastCompletedAt, record.getCompletedAt());
			saleOfferKeys.add(record.getSaleOfferKey() == null
				? record.getItemId() + ":" + record.getCompletedAt()
				: record.getSaleOfferKey());
		}

		private int getItemId()
		{
			return itemId;
		}

		private String getItemName()
		{
			return itemName;
		}

		private long getQuantity()
		{
			return quantity;
		}

		private long getCost()
		{
			return cost;
		}

		private long getProfit()
		{
			return profit;
		}

		private long getLastCompletedAt()
		{
			return lastCompletedAt;
		}

		private int getFlipCount()
		{
			return saleOfferKeys.size();
		}
	}

	private static class CoinBadge extends JComponent
	{
		private CoinBadge()
		{
			Dimension size = new Dimension(26, 26);
			setMinimumSize(size);
			setPreferredSize(size);
			setMaximumSize(size);
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(31, 29, 24));
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
			drawCoin(g, 6, 14, 15, 6);
			drawCoin(g, 6, 10, 15, 6);
			drawCoin(g, 6, 6, 15, 6);
			g.dispose();
		}

		private void drawCoin(Graphics2D g, int x, int y, int width, int height)
		{
			g.setColor(new Color(124, 82, 16));
			g.fillOval(x, y + 2, width, height);
			g.setColor(TAB_ACCENT);
			g.fillOval(x, y, width, height);
			g.setColor(new Color(124, 82, 16));
			g.setStroke(new BasicStroke(1.2f));
			g.drawOval(x, y, width, height);
			g.setColor(new Color(255, 225, 118));
			g.drawArc(x + 3, y + 1, width - 7, height - 3, 25, 130);
		}
	}

	private static class ChartButton extends JButton
	{
		private ChartButton()
		{
			setPreferredSize(new Dimension(HEART_BUTTON_SIZE, HEART_BUTTON_SIZE));
			setMinimumSize(new Dimension(HEART_BUTTON_SIZE, HEART_BUTTON_SIZE));
			setMaximumSize(new Dimension(HEART_BUTTON_SIZE, HEART_BUTTON_SIZE));
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setRolloverEnabled(true);
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			if (getModel().isRollover() || getModel().isPressed())
			{
				g.setColor(getModel().isPressed() ? ColorScheme.DARK_GRAY_COLOR : new Color(42, 42, 42));
				g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 2, 2);
			}

			int left = 5;
			int bottom = getHeight() - 6;
			g.setColor(new Color(95, 95, 95));
			g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(left, 5, left, bottom);
			g.drawLine(left, bottom, getWidth() - 5, bottom);

			Path2D line = new Path2D.Double();
			line.moveTo(left + 1, bottom - 3);
			line.lineTo(left + 5, bottom - 7);
			line.lineTo(left + 9, bottom - 5);
			line.lineTo(left + 13, bottom - 11);
			g.setColor(TAB_ACCENT);
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(line);
			g.dispose();
		}
	}

	private static class SearchButton extends JButton
	{
		private SearchButton()
		{
			fixedButtonSize(this, 28, CONTROL_HEIGHT);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setRolloverEnabled(true);
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			drawUtilityBackground(g, this);
			g.setColor(getModel().isRollover() ? Color.WHITE : Color.LIGHT_GRAY);
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawOval(8, 6, 9, 9);
			g.drawLine(16, 14, 20, 18);
			g.dispose();
		}
	}

	private static class RefreshButton extends JButton
	{
		private RefreshButton()
		{
			fixedButtonSize(this, 34, 34);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setRolloverEnabled(true);
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			drawUtilityBackground(g, this);
			g.setColor(getModel().isRollover() ? Color.WHITE : Color.LIGHT_GRAY);
			g.setStroke(new BasicStroke(1.85f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawArc(9, 9, 16, 16, 38, 276);

			Path2D arrow = new Path2D.Double();
			arrow.moveTo(26.0, 8.4);
			arrow.lineTo(25.4, 14.7);
			arrow.lineTo(19.8, 11.5);
			arrow.closePath();
			g.fill(arrow);
			g.dispose();
		}
	}

	private static class MarketNavButton extends JButton
	{
		private final String icon;
		private final boolean selected;
		private final boolean expanded;

		private MarketNavButton(String icon, boolean selected, boolean expanded)
		{
			this.icon = icon;
			this.selected = selected;
			this.expanded = expanded;
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setRolloverEnabled(true);
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			boolean hovered = getModel().isRollover();
			if (hovered)
			{
				g.setColor(expanded ? new Color(53, 53, 53) : new Color(48, 48, 48));
				g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 4, 4);
				if (!expanded)
				{
					g.setColor(new Color(67, 67, 67));
					g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 4, 4);
				}
			}
			else if (expanded)
			{
				g.setColor(new Color(43, 43, 43));
				g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 4, 4);
			}
			if (expanded)
			{
				g.setColor(TAB_ACCENT);
				g.fillRect(6, getHeight() - 2, getWidth() - 12, 2);
			}
			g.setColor(selected ? TAB_ACCENT : hovered ? Color.WHITE : Color.LIGHT_GRAY);
			g.setStroke(new BasicStroke(1.45f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int center = getWidth() / 2;
			if ("calculator".equals(icon))
			{
				int x = center - 8;
				int y = 5;
				g.drawRoundRect(x, y, 16, 18, 2, 2);
				g.setColor(TAB_ACCENT);
				g.fillRoundRect(x + 3, y + 3, 10, 4, 1, 1);
				g.setColor(selected ? TAB_ACCENT : hovered ? Color.WHITE : Color.LIGHT_GRAY);
				for (int row = 0; row < 3; row++)
				{
					for (int col = 0; col < 3; col++)
					{
						g.fillRect(x + 3 + col * 4, y + 9 + row * 3, 2, 2);
					}
				}
			}
			else if ("favorites".equals(icon))
			{
				Path2D heart = new Path2D.Double();
				heart.moveTo(center, 22);
				heart.curveTo(center - 2, 19, center - 9, 15, center - 9, 10);
				heart.curveTo(center - 9, 6, center - 4, 5, center, 9);
				heart.curveTo(center + 4, 5, center + 9, 6, center + 9, 10);
				heart.curveTo(center + 9, 15, center + 2, 19, center, 22);
				g.draw(heart);
			}
			else if ("highVolume".equals(icon))
			{
				g.fillRect(center - 9, 15, 4, 7);
				g.fillRect(center - 2, 10, 4, 12);
				g.fillRect(center + 5, 6, 4, 16);
			}
			else if ("screener".equals(icon))
			{
				Path2D funnel = new Path2D.Double();
				funnel.moveTo(center - 10, 7);
				funnel.lineTo(center + 10, 7);
				funnel.lineTo(center + 3, 14);
				funnel.lineTo(center + 3, 21);
				funnel.lineTo(center - 3, 18);
				funnel.lineTo(center - 3, 14);
				funnel.closePath();
				g.draw(funnel);
			}
			else
			{
				int x = center - 8;
				g.drawRoundRect(x, 6, 16, 17, 2, 2);
				g.drawLine(x + 4, 11, x + 12, 11);
				g.drawLine(x + 4, 15, x + 12, 15);
				g.drawLine(x + 4, 19, x + 9, 19);
			}
			g.dispose();
		}
	}

	private static class PageArrowButton extends JButton
	{
		private final boolean next;

		private PageArrowButton(boolean next)
		{
			this.next = next;
			fixedButtonSize(this, 36, 24);
			setToolTipText(next ? "Next page." : "Previous page.");
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			drawUtilityBackground(g, this);
			g.setColor(isEnabled() ? (getModel().isRollover() ? Color.WHITE : Color.LIGHT_GRAY) : new Color(76, 76, 76));
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int center = getWidth() / 2;
			int direction = next ? 1 : -1;
			g.drawLine(center - direction * 3, 8, center + direction * 2, 12);
			g.drawLine(center + direction * 2, 12, center - direction * 3, 16);
			g.dispose();
		}
	}

	private static class PinnedAreaButton extends JButton
	{
		private final boolean restore;

		private PinnedAreaButton(boolean restore)
		{
			this.restore = restore;
			fixedButtonSize(this, 28, CONTROL_HEIGHT);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setRolloverEnabled(true);
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			drawUtilityBackground(g, this);
			g.setColor(getModel().isRollover() ? Color.WHITE : Color.LIGHT_GRAY);
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int center = getWidth() / 2;
			if (restore)
			{
				g.drawLine(center - 4, 9, center, 14);
				g.drawLine(center, 14, center + 4, 9);
			}
			else
			{
				g.drawLine(center - 5, 12, center + 5, 12);
			}
			g.dispose();
		}
	}

	private static class FilterCheckIcon implements Icon
	{
		private static final int SIZE = 19;
		private final boolean checked;

		private FilterCheckIcon(boolean checked)
		{
			this.checked = checked;
		}

		@Override
		public int getIconWidth()
		{
			return SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return SIZE;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			boolean hovered = component instanceof JCheckBox
				&& ((JCheckBox) component).getModel().isRollover();
			g.setColor(checked ? new Color(37, 37, 37) : INPUT_SURFACE);
			g.fillRect(x + 1, y + 1, SIZE - 2, SIZE - 2);
			g.setColor(checked
				? new Color(94, 94, 94)
				: hovered ? new Color(96, 96, 96) : new Color(67, 67, 67));
			g.drawRect(x + 1, y + 1, SIZE - 3, SIZE - 3);

			if (checked)
			{
				g.setColor(hovered ? Color.WHITE : Color.LIGHT_GRAY);
				g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				Path2D check = new Path2D.Double();
				check.moveTo(x + 5, y + 10);
				check.lineTo(x + 8, y + 13);
				check.lineTo(x + 14, y + 6);
				g.draw(check);
			}
			g.dispose();
		}
	}

	private static void fixedButtonSize(JButton button, int width, int height)
	{
		Dimension size = new Dimension(width, height);
		button.setMinimumSize(size);
		button.setPreferredSize(size);
		button.setMaximumSize(size);
	}

	private static void drawUtilityBackground(Graphics2D g, JButton button)
	{
		if (button.getModel().isPressed())
		{
			g.setColor(new Color(25, 25, 25));
		}
		else if (button.getModel().isRollover())
		{
			g.setColor(new Color(48, 48, 48));
		}
		else
		{
			g.setColor(INPUT_SURFACE);
		}
		g.fillRoundRect(1, 1, button.getWidth() - 2, button.getHeight() - 2, 4, 4);
		g.setColor(INPUT_BORDER);
		g.drawRoundRect(1, 1, button.getWidth() - 3, button.getHeight() - 3, 4, 4);
	}

	private static class CloseButton extends JButton
	{
		private CloseButton()
		{
			setPreferredSize(new Dimension(CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE));
			setMinimumSize(new Dimension(CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE));
			setMaximumSize(new Dimension(CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE));
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			if (getModel().isRollover() || getModel().isPressed())
			{
				g.setColor(getModel().isPressed() ? ColorScheme.DARK_GRAY_COLOR : new Color(42, 42, 42));
				g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 2, 2);
			}

			g.setColor(getModel().isRollover() ? Color.WHITE : Color.LIGHT_GRAY);
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int pad = 7;
			g.drawLine(pad, pad, getWidth() - pad, getHeight() - pad);
			g.drawLine(getWidth() - pad, pad, pad, getHeight() - pad);
			g.dispose();
		}
	}

	private static class DailyChartPanel extends JPanel
	{
		private static final Color LOW_PRICE_COLOR = new Color(94, 166, 255);
		private static final Color HIGH_PRICE_COLOR = new Color(255, 157, 67);
		private static final Color VOLUME_HIGH_COLOR = new Color(255, 157, 67, 185);
		private static final Color VOLUME_LOW_COLOR = new Color(94, 166, 255, 185);
		private static final int SPARSE_POINT_THRESHOLD = 72;
		private static final DateTimeFormatter CHART_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US)
			.withZone(ZoneId.systemDefault());
		private static final DateTimeFormatter AXIS_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
		private static final DateTimeFormatter AXIS_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US);
		private final List<TimeseriesPoint> points;
		private final ChartPeriod period;
		private int hoverIndex = -1;
		private int hoverY = -1;

		private DailyChartPanel(List<TimeseriesPoint> points, ChartPeriod period)
		{
			this.points = points == null ? new ArrayList<>() : new ArrayList<>(points);
			this.points.sort(Comparator.comparingLong(TimeseriesPoint::getTimestamp));
			this.period = period;
			limitToPeriodWindow();
			setOpaque(true);
			setBackground(new Color(24, 24, 24));
			setBorder(BorderFactory.createLineBorder(new Color(45, 45, 45)));
			setToolTipText(" ");
			addMouseMotionListener(new MouseMotionAdapter()
			{
				@Override
				public void mouseMoved(MouseEvent event)
				{
					updateHover(event.getX(), event.getY());
				}
			});
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseExited(MouseEvent event)
				{
					hoverIndex = -1;
					hoverY = -1;
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			List<TimeseriesPoint> chartPoints = chartPoints();
			List<TimeseriesPoint> lowPoints = pricePoints(false);
			List<TimeseriesPoint> highPoints = pricePoints(true);
			if (lowPoints.size() < 2 && highPoints.size() < 2)
			{
				drawCentered(g, "No " + period.label.toLowerCase(Locale.US) + " chart data returned.");
				g.dispose();
				return;
			}

			ChartLayout layout = chartLayout();
			Range range = priceRange(lowPoints, highPoints);
			TimeRange timeRange = timeRange(chartPoints);
			drawGrid(g, layout, range);
			drawVolume(g, chartPoints, layout, timeRange);
			drawTimeAxis(g, layout, timeRange);
			drawPriceLine(g, lowPoints, false, layout, range, timeRange, LOW_PRICE_COLOR);
			drawPriceLine(g, highPoints, true, layout, range, timeRange, HIGH_PRICE_COLOR);
			drawPriceDots(g, lowPoints, false, layout, range, timeRange, LOW_PRICE_COLOR);
			drawPriceDots(g, highPoints, true, layout, range, timeRange, HIGH_PRICE_COLOR);
			drawSparseLabel(g, lowPoints, highPoints, layout);
			List<TimeseriesPoint> hoverPoints = hoverPoints(hoverY, layout);
			if (hoverIndex >= 0 && hoverIndex < hoverPoints.size())
			{
				drawHover(g, hoverPoints, hoverIndex, layout, range, timeRange);
			}

			g.setFont(STAT_LABEL_FONT);
			g.setColor(LOW_PRICE_COLOR);
			g.drawString("Buy", layout.left, layout.volumeTop - 8);
			g.setColor(HIGH_PRICE_COLOR);
			g.drawString("Sell", layout.left + 58, layout.volumeTop - 8);
			g.dispose();
		}

		@Override
		public String getToolTipText(MouseEvent event)
		{
			return null;
		}

		private void updateHover(int mouseX, int mouseY)
		{
			ChartLayout layout = chartLayout();
			List<TimeseriesPoint> hoverPoints = hoverPoints(mouseY, layout);
			if (hoverPoints.size() < 2)
			{
				return;
			}
			int updated = nearestIndex(mouseX, hoverPoints, layout, timeRange(chartPoints()));
			if (updated != hoverIndex || mouseY != hoverY)
			{
				hoverIndex = updated;
				hoverY = mouseY;
				repaint();
			}
		}

		private ChartLayout chartLayout()
		{
			int left = 64;
			int right = 28;
			int top = 28;
			int priceBottom = getHeight() - 198;
			int volumeTop = getHeight() - 170;
			int volumeBottom = getHeight() - 66;
			int plotWidth = Math.max(1, getWidth() - left - right);
			int plotHeight = Math.max(1, priceBottom - top);
			return new ChartLayout(left, top, priceBottom, volumeTop, volumeBottom, plotWidth, plotHeight);
		}

		private void limitToPeriodWindow()
		{
			if (points.isEmpty())
			{
				return;
			}
			long latestTimestamp = points.get(points.size() - 1).getTimestamp();
			long cutoff = latestTimestamp - period.durationSeconds;
			points.removeIf(point -> point == null || point.getTimestamp() < cutoff);
		}

		private List<TimeseriesPoint> chartPoints()
		{
			List<TimeseriesPoint> chart = new ArrayList<>();
			for (TimeseriesPoint point : points)
			{
				if (point != null && point.getTimestamp() > 0 && (hasAnyPrice(point) || point.getTotalVolume() > 0))
				{
					chart.add(point);
				}
			}
			return chart;
		}

		private List<TimeseriesPoint> pricePoints(boolean high)
		{
			List<TimeseriesPoint> priced = new ArrayList<>();
			for (TimeseriesPoint point : points)
			{
				if (point != null && point.getTimestamp() > 0 && hasPrice(point, high))
				{
					priced.add(point);
				}
			}
			return priced;
		}

		private List<TimeseriesPoint> hoverPoints(int mouseY, ChartLayout layout)
		{
			if (isVolumeHover(mouseY, layout))
			{
				return chartPoints();
			}
			List<TimeseriesPoint> priced = new ArrayList<>();
			for (TimeseriesPoint point : points)
			{
				if (point != null && point.getTimestamp() > 0 && hasAnyPrice(point))
				{
					priced.add(point);
				}
			}
			return priced;
		}

		private Range priceRange(List<TimeseriesPoint> lowPoints, List<TimeseriesPoint> highPoints)
		{
			int min = Integer.MAX_VALUE;
			int max = Integer.MIN_VALUE;
			for (TimeseriesPoint point : lowPoints)
			{
				min = Math.min(min, point.getAvgLowPrice());
				max = Math.max(max, point.getAvgLowPrice());
			}
			for (TimeseriesPoint point : highPoints)
			{
				min = Math.min(min, point.getAvgHighPrice());
				max = Math.max(max, point.getAvgHighPrice());
			}
			if (min == Integer.MAX_VALUE || max == Integer.MIN_VALUE)
			{
				min = 0;
				max = 1;
			}
			if (min == max)
			{
				min = Math.max(0, min - 1);
				max++;
			}
			int padding = Math.max(1, (max - min) / 12);
			return new Range(Math.max(0, min - padding), max + padding);
		}

		private TimeRange timeRange(List<TimeseriesPoint> chartPoints)
		{
			long min = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			for (TimeseriesPoint point : chartPoints)
			{
				if (point != null && point.getTimestamp() > 0)
				{
					min = Math.min(min, point.getTimestamp());
					max = Math.max(max, point.getTimestamp());
				}
			}
			if (min == Long.MAX_VALUE || max == Long.MIN_VALUE)
			{
				min = 0;
				max = period.intervalSeconds;
			}
			if (min == max)
			{
				min -= period.intervalSeconds;
				max += period.intervalSeconds;
			}
			return new TimeRange(min, max);
		}

		private void drawGrid(Graphics2D g, ChartLayout layout, Range range)
		{
			g.setFont(STAT_LABEL_FONT);
			g.setStroke(new BasicStroke(1f));
			for (int i = 0; i <= 4; i++)
			{
				int y = layout.top + (layout.plotHeight * i / 4);
				int price = range.max - ((range.max - range.min) * i / 4);
				g.setColor(new Color(42, 42, 42));
				g.drawLine(layout.left, y, layout.left + layout.plotWidth, y);
				g.setColor(Color.GRAY);
				g.drawString(axisGp(price), 7, y + 4);
			}
		}

		private void drawVolume(Graphics2D g, List<TimeseriesPoint> priced, ChartLayout layout, TimeRange timeRange)
		{
			int volumeScale = volumeScale(priced);
			if (volumeScale <= 0)
			{
				return;
			}

			int barWidth = volumeBarWidth(layout, timeRange);
			int volumeLeft = layout.left - barWidth / 2;
			int volumeWidth = layout.plotWidth + barWidth;
			g.setColor(new Color(35, 35, 35));
			g.fillRect(volumeLeft, layout.volumeTop, volumeWidth, layout.volumeBottom - layout.volumeTop);
			g.setColor(new Color(72, 72, 72));
			g.drawLine(volumeLeft, layout.volumeBaseline(), volumeLeft + volumeWidth, layout.volumeBaseline());

			int halfHeight = Math.max(1, (layout.volumeBottom - layout.volumeTop) / 2 - 3);
			for (int i = 0; i < priced.size(); i++)
			{
				TimeseriesPoint point = priced.get(i);
				int x = xForTime(point.getTimestamp(), timeRange, layout) - barWidth / 2;
				int highBarHeight = volumeBarHeight(point.getHighPriceVolume(), volumeScale, halfHeight);
				int lowBarHeight = volumeBarHeight(point.getLowPriceVolume(), volumeScale, halfHeight);
				if (highBarHeight > 0)
				{
					g.setColor(VOLUME_HIGH_COLOR);
					g.fillRect(x, layout.volumeBaseline() - highBarHeight, barWidth, highBarHeight);
				}
				if (lowBarHeight > 0)
				{
					g.setColor(VOLUME_LOW_COLOR);
					g.fillRect(x, layout.volumeBaseline() + 1, barWidth, lowBarHeight);
				}
			}
		}

		private void drawTimeAxis(Graphics2D g, ChartLayout layout, TimeRange timeRange)
		{
			if (period == ChartPeriod.DAILY)
			{
				drawDailyTimeAxis(g, layout, timeRange);
			}
			else
			{
				drawWeeklyTimeAxis(g, layout, timeRange);
			}
		}

		private void drawDailyTimeAxis(Graphics2D g, ChartLayout layout, TimeRange timeRange)
		{
			ZoneId zone = ZoneId.systemDefault();
			ZonedDateTime tick = Instant.ofEpochSecond(timeRange.min).atZone(zone)
				.withMinute(0)
				.withSecond(0)
				.withNano(0);
			int remainder = tick.getHour() % 3;
			if (remainder != 0)
			{
				tick = tick.plusHours(3 - remainder);
			}
			if (tick.toEpochSecond() < timeRange.min)
			{
				tick = tick.plusHours(3);
			}

			g.setFont(STAT_LABEL_FONT);
			g.setStroke(new BasicStroke(1f));
			while (tick.toEpochSecond() <= timeRange.max)
			{
				int x = xForTime(tick.toEpochSecond(), timeRange, layout);
				g.setColor(new Color(82, 82, 82));
				g.drawLine(x, layout.volumeBottom + 2, x, layout.volumeBottom + 7);

				String timeText = AXIS_TIME.format(tick).toLowerCase(Locale.US);
				String dateText = AXIS_DATE.format(tick);
				g.setColor(Color.LIGHT_GRAY);
				drawCenteredAxisText(g, timeText, x, layout.volumeBottom + 20);
				g.setColor(Color.GRAY);
				drawCenteredAxisText(g, dateText, x, layout.volumeBottom + 35);
				tick = tick.plusHours(3);
			}
		}

		private void drawWeeklyTimeAxis(Graphics2D g, ChartLayout layout, TimeRange timeRange)
		{
			ZoneId zone = ZoneId.systemDefault();
			ZonedDateTime tick = Instant.ofEpochSecond(timeRange.min).atZone(zone)
				.toLocalDate()
				.plusDays(1)
				.atStartOfDay(zone);

			g.setFont(STAT_LABEL_FONT);
			g.setStroke(new BasicStroke(1f));
			while (tick.toEpochSecond() <= timeRange.max)
			{
				int x = xForTime(tick.toEpochSecond(), timeRange, layout);
				g.setColor(new Color(82, 82, 82));
				g.drawLine(x, layout.volumeBottom + 2, x, layout.volumeBottom + 7);
				g.setColor(Color.LIGHT_GRAY);
				drawCenteredAxisText(g, AXIS_DATE.format(tick), x, layout.volumeBottom + 24);
				tick = tick.plusDays(1);
			}
		}

		private void drawCenteredAxisText(Graphics2D g, String text, int centerX, int baseline)
		{
			int textWidth = g.getFontMetrics().stringWidth(text);
			int x = centerX - textWidth / 2;
			x = Math.max(4, Math.min(x, getWidth() - textWidth - 4));
			g.drawString(text, x, baseline);
		}

		private int volumeScale(List<TimeseriesPoint> priced)
		{
			List<Integer> volumes = new ArrayList<>();
			for (TimeseriesPoint point : priced)
			{
				if (point.getHighPriceVolume() > 0)
				{
					volumes.add(point.getHighPriceVolume());
				}
				if (point.getLowPriceVolume() > 0)
				{
					volumes.add(point.getLowPriceVolume());
				}
			}
			if (volumes.isEmpty())
			{
				return 0;
			}

			Collections.sort(volumes);
			int index = Math.max(0, Math.min(volumes.size() - 1, (int) Math.ceil(volumes.size() * 0.92) - 1));
			return Math.max(1, volumes.get(index));
		}

		private int volumeBarHeight(int volume, int scale, int halfHeight)
		{
			if (volume <= 0 || scale <= 0)
			{
				return 0;
			}
			double ratio = Math.sqrt(Math.min(volume, scale) / (double) scale);
			return Math.max(2, (int) Math.round(halfHeight * ratio));
		}

		private void drawPriceLine(
			Graphics2D g,
			List<TimeseriesPoint> series,
			boolean high,
			ChartLayout layout,
			Range range,
			TimeRange timeRange,
			Color color)
		{
			for (int i = 1; i < series.size(); i++)
			{
				TimeseriesPoint previous = series.get(i - 1);
				TimeseriesPoint point = series.get(i);
				int previousPrice = high ? previous.getAvgHighPrice() : previous.getAvgLowPrice();
				int price = high ? point.getAvgHighPrice() : point.getAvgLowPrice();
				int previousX = xForTime(previous.getTimestamp(), timeRange, layout);
				int x = xForTime(point.getTimestamp(), timeRange, layout);
				int previousY = yForPrice(previousPrice, range, layout);
				int y = yForPrice(price, range, layout);
				g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 238));
				g.setStroke(new BasicStroke(1.65f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.drawLine(previousX, previousY, x, y);
			}
		}

		private void drawPriceDots(
			Graphics2D g,
			List<TimeseriesPoint> series,
			boolean high,
			ChartLayout layout,
			Range range,
			TimeRange timeRange,
			Color color)
		{
			int diameter = series.size() > 325 ? 4 : 5;
			for (TimeseriesPoint point : series)
			{
				int price = high ? point.getAvgHighPrice() : point.getAvgLowPrice();
				int x = xForTime(point.getTimestamp(), timeRange, layout);
				int y = yForPrice(price, range, layout);
				g.setColor(new Color(18, 18, 18, 185));
				g.fillOval(x - (diameter + 2) / 2, y - (diameter + 2) / 2, diameter + 2, diameter + 2);
				g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 245));
				g.fillOval(x - diameter / 2, y - diameter / 2, diameter, diameter);
			}
		}

		private void drawSparseLabel(Graphics2D g, List<TimeseriesPoint> lowPoints, List<TimeseriesPoint> highPoints, ChartLayout layout)
		{
			if (lowPoints.size() >= SPARSE_POINT_THRESHOLD && highPoints.size() >= SPARSE_POINT_THRESHOLD)
			{
				return;
			}

			String text = "Sparse " + period.intervalLabel + " history: " + lowPoints.size() + " buy / " + highPoints.size() + " sell points";
			g.setFont(STAT_LABEL_FONT);
			int paddingX = 7;
			int width = g.getFontMetrics().stringWidth(text) + paddingX * 2;
			int height = 20;
			int x = Math.max(layout.left, layout.left + layout.plotWidth - width);
			int y = layout.top + 5;
			g.setColor(new Color(42, 42, 42, 226));
			g.fillRoundRect(x, y, width, height, 5, 5);
			g.setColor(new Color(122, 122, 122));
			g.drawRoundRect(x, y, width, height, 5, 5);
			g.setColor(new Color(214, 177, 92));
			g.drawString(text, x + paddingX, y + 14);
		}

		private void drawHover(Graphics2D g, List<TimeseriesPoint> priced, int index, ChartLayout layout, Range range, TimeRange timeRange)
		{
			TimeseriesPoint point = priced.get(index);
			int x = xForTime(point.getTimestamp(), timeRange, layout);

			g.setColor(new Color(210, 210, 210, 110));
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f));
			g.drawLine(x, layout.top, x, layout.volumeBottom);
			if (isVolumeHover(hoverY, layout))
			{
				drawVolumeHover(g, point, x, layout);
			}
			else
			{
				if (hasPrice(point, false))
				{
					int lowY = yForPrice(point.getAvgLowPrice(), range, layout);
					drawPriceGuideLine(g, lowY, LOW_PRICE_COLOR, layout);
					drawPointMarker(g, x, lowY, LOW_PRICE_COLOR);
					drawPriceTag(g, "Buy " + GP.format(point.getAvgLowPrice()) + " gp", x, lowY, LOW_PRICE_COLOR, layout);
				}
				if (hasPrice(point, true))
				{
					int highY = yForPrice(point.getAvgHighPrice(), range, layout);
					drawPriceGuideLine(g, highY, HIGH_PRICE_COLOR, layout);
					drawPointMarker(g, x, highY, HIGH_PRICE_COLOR);
					drawPriceTag(g, "Sell " + GP.format(point.getAvgHighPrice()) + " gp", x, highY, HIGH_PRICE_COLOR, layout);
				}
			}
			drawTimeTag(g, chartTime(point), x);
		}

		private boolean hasAnyPrice(TimeseriesPoint point)
		{
			return hasPrice(point, false) || hasPrice(point, true);
		}

		private boolean hasPrice(TimeseriesPoint point, boolean high)
		{
			Integer price = high ? point.getAvgHighPrice() : point.getAvgLowPrice();
			return price != null && price > 0;
		}

		private void drawPriceGuideLine(Graphics2D g, int y, Color color, ChartLayout layout)
		{
			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 95));
			g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f));
			g.drawLine(layout.left, y, layout.left + layout.plotWidth, y);
		}

		private boolean isVolumeHover(int y, ChartLayout layout)
		{
			return y >= layout.volumeTop - 8 && y <= layout.volumeBottom + 8;
		}

		private void drawPointMarker(Graphics2D g, int x, int y, Color color)
		{
			g.setColor(new Color(24, 24, 24));
			g.fillOval(x - 5, y - 5, 10, 10);
			g.setColor(color);
			g.fillOval(x - 3, y - 3, 6, 6);
		}

		private void drawPriceTag(Graphics2D g, String text, int anchorX, int anchorY, Color color, ChartLayout layout)
		{
			g.setFont(BODY_FONT);
			int paddingX = 7;
			int height = 20;
			int width = g.getFontMetrics().stringWidth(text) + paddingX * 2;
			int x = anchorX + 8;
			if (x + width > getWidth() - 7)
			{
				x = anchorX - width - 8;
			}
			x = Math.max(7, Math.min(x, getWidth() - width - 7));
			int y = Math.max(layout.top, Math.min(anchorY - height / 2, layout.priceBottom - height));

			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 225));
			g.fillRoundRect(x, y, width, height, 4, 4);
			g.setColor(new Color(10, 10, 10, 140));
			g.drawRoundRect(x, y, width, height, 4, 4);
			g.setColor(Color.WHITE);
			g.drawString(text, x + paddingX, y + 14);
		}

		private void drawVolumeHover(Graphics2D g, TimeseriesPoint point, int anchorX, ChartLayout layout)
		{
			g.setColor(new Color(235, 235, 235, 150));
			g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f));
			g.drawLine(anchorX, layout.volumeTop - 2, anchorX, layout.volumeBottom + 2);

			String highText = "High-price vol: " + GP.format(point.getHighPriceVolume());
			String lowText = "Low-price vol: " + GP.format(point.getLowPriceVolume());
			String totalText = "Total vol: " + GP.format(point.getTotalVolume());
			g.setFont(BODY_FONT);
			int paddingX = 8;
			int lineHeight = 15;
			int width = Math.max(
				g.getFontMetrics().stringWidth(highText),
				Math.max(g.getFontMetrics().stringWidth(lowText), g.getFontMetrics().stringWidth(totalText))
			) + paddingX * 2;
			int height = 54;
			int x = anchorX + 10;
			if (x + width > getWidth() - 7)
			{
				x = anchorX - width - 10;
			}
			x = Math.max(7, Math.min(x, getWidth() - width - 7));
			int y = Math.max(layout.top, layout.volumeTop - height - 10);

			g.setColor(new Color(46, 46, 46, 238));
			g.fillRoundRect(x, y, width, height, 5, 5);
			g.setColor(new Color(100, 100, 100));
			g.drawRoundRect(x, y, width, height, 5, 5);
			g.setColor(Color.LIGHT_GRAY);
			g.drawString(highText, x + paddingX, y + 15);
			g.drawString(lowText, x + paddingX, y + 15 + lineHeight);
			g.setColor(Color.WHITE);
			g.drawString(totalText, x + paddingX, y + 15 + lineHeight * 2);
		}

		private void drawTimeTag(Graphics2D g, String text, int anchorX)
		{
			g.setFont(BODY_FONT);
			int paddingX = 7;
			int height = 22;
			int width = g.getFontMetrics().stringWidth(text) + paddingX * 2;
			int x = Math.max(7, Math.min(anchorX - width / 2, getWidth() - width - 7));
			int y = getHeight() - 35;

			Path2D pointer = new Path2D.Double();
			pointer.moveTo(anchorX, y - 5);
			pointer.lineTo(anchorX - 6, y + 1);
			pointer.lineTo(anchorX + 6, y + 1);
			pointer.closePath();
			g.setColor(new Color(58, 58, 58));
			g.fill(pointer);
			g.fillRoundRect(x, y, width, height, 3, 3);
			g.setColor(Color.WHITE);
			g.drawString(text, x + paddingX, y + 15);
		}

		private int nearestIndex(int mouseX, List<TimeseriesPoint> priced, ChartLayout layout, TimeRange timeRange)
		{
			int nearest = 0;
			int bestDistance = Integer.MAX_VALUE;
			for (int i = 0; i < priced.size(); i++)
			{
				int distance = Math.abs(xForTime(priced.get(i).getTimestamp(), timeRange, layout) - mouseX);
				if (distance < bestDistance)
				{
					nearest = i;
					bestDistance = distance;
				}
			}
			return nearest;
		}

		private int xForTime(long timestamp, TimeRange range, ChartLayout layout)
		{
			double ratio = (timestamp - range.min) / (double) Math.max(1L, range.max - range.min);
			ratio = Math.max(0, Math.min(1, ratio));
			return layout.left + (int) Math.round(ratio * layout.plotWidth);
		}

		private int volumeBarWidth(ChartLayout layout, TimeRange range)
		{
			int intervalWidth = xForTime(range.min + period.intervalSeconds, range, layout) - xForTime(range.min, range, layout);
			return Math.max(2, Math.min(12, intervalWidth));
		}

		private int yForPrice(int price, Range range, ChartLayout layout)
		{
			double ratio = (price - range.min) / (double) Math.max(1, range.max - range.min);
			return layout.priceBottom - (int) Math.round(ratio * (layout.priceBottom - layout.top));
		}

		private void drawCentered(Graphics2D g, String text)
		{
			g.setFont(BODY_FONT);
			g.setColor(Color.LIGHT_GRAY);
			int width = g.getFontMetrics().stringWidth(text);
			g.drawString(text, Math.max(8, (getWidth() - width) / 2), Math.max(18, getHeight() / 2));
		}

		private String compactGp(int value)
		{
			if (value >= 1_000_000)
			{
				return String.format(Locale.US, "%.1fm", value / 1_000_000.0);
			}
			if (value >= 10_000)
			{
				return (value / 1000) + "k";
			}
			return GP.format(value);
		}

		private String axisGp(int value)
		{
			if (value >= 1_000_000)
			{
				return String.format(Locale.US, "%.2fm", value / 1_000_000.0);
			}
			if (value >= 100_000)
			{
				return String.format(Locale.US, "%.1fk", value / 1000.0);
			}
			if (value >= 10_000)
			{
				return String.format(Locale.US, "%.1fk", value / 1000.0);
			}
			return GP.format(value);
		}

		private String chartTime(TimeseriesPoint point)
		{
			return CHART_TIME.format(Instant.ofEpochSecond(point.getTimestamp()));
		}

		private static class ChartLayout
		{
			private final int left;
			private final int top;
			private final int priceBottom;
			private final int volumeTop;
			private final int volumeBottom;
			private final int plotWidth;
			private final int plotHeight;

			private ChartLayout(int left, int top, int priceBottom, int volumeTop, int volumeBottom, int plotWidth, int plotHeight)
			{
				this.left = left;
				this.top = top;
				this.priceBottom = priceBottom;
				this.volumeTop = volumeTop;
				this.volumeBottom = volumeBottom;
				this.plotWidth = plotWidth;
				this.plotHeight = plotHeight;
			}

			private int volumeBaseline()
			{
				return volumeTop + (volumeBottom - volumeTop) / 2;
			}
		}

		private static class TimeRange
		{
			private final long min;
			private final long max;

			private TimeRange(long min, long max)
			{
				this.min = min;
				this.max = max;
			}
		}

		private static class Range
		{
			private final int min;
			private final int max;

			private Range(int min, int max)
			{
				this.min = min;
				this.max = max;
			}
		}
	}

	private static class HeartButton extends JButton
	{
		private final boolean favorite;

		private HeartButton(boolean favorite)
		{
			this.favorite = favorite;
			setPreferredSize(new Dimension(HEART_BUTTON_SIZE, HEART_BUTTON_SIZE));
			setMinimumSize(new Dimension(HEART_BUTTON_SIZE, HEART_BUTTON_SIZE));
			setMaximumSize(new Dimension(HEART_BUTTON_SIZE, HEART_BUTTON_SIZE));
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setMargin(new Insets(0, 0, 0, 0));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			if (getModel().isRollover() || getModel().isPressed())
			{
				g.setColor(getModel().isPressed() ? ColorScheme.DARK_GRAY_COLOR : new Color(42, 42, 42));
				g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 2, 2);
			}

			double width = 12.0;
			double height = 10.5;
			double x = (getWidth() - width) / 2.0;
			double y = (getHeight() - height) / 2.0;
			double center = x + width / 2.0;
			Path2D heart = new Path2D.Double();
			heart.moveTo(center, y + height);
			heart.curveTo(x + 0.2, y + height * 0.62, x - 0.4, y + height * 0.25, x + 1.2, y + height * 0.10);
			heart.curveTo(x + 2.7, y - 0.6, x + 4.5, y + 0.2, center, y + height * 0.32);
			heart.curveTo(x + width - 4.5, y + 0.2, x + width - 2.7, y - 0.6, x + width - 1.2, y + height * 0.10);
			heart.curveTo(x + width + 0.4, y + height * 0.25, x + width - 0.2, y + height * 0.62, center, y + height);
			if (favorite)
			{
				g.setColor(TAB_ACCENT);
				g.fill(heart);
			}
			else
			{
				g.setColor(Color.LIGHT_GRAY);
				g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(heart);
			}
			g.dispose();
		}
	}

	private static class ScrollableContentPanel extends JPanel implements Scrollable
	{
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(48, visibleRect.height - 32);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

}
