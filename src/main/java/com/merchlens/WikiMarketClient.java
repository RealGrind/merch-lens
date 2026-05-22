package com.merchlens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merchlens.model.ItemMetadata;
import com.merchlens.model.LatestPrice;
import com.merchlens.model.MarketItem;
import com.merchlens.model.OfferAdvice;
import com.merchlens.model.FlipHistorySummary;
import com.merchlens.model.FlipRecord;
import com.merchlens.model.HistoricalSignal;
import com.merchlens.model.PriceWindow;
import com.merchlens.model.ItemSearchResult;
import com.merchlens.model.RecommendationDto;
import com.merchlens.model.SignalResponse;
import com.merchlens.model.TimeseriesPoint;
import com.merchlens.model.TimeseriesResponse;
import com.merchlens.model.WikiEnvelope;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WikiMarketClient
{
	private static final String WIKI_BASE = "https://prices.runescape.wiki/api/v1/osrs";
	private static final Type LATEST_TYPE = new TypeToken<WikiEnvelope<LatestPrice>>() {}.getType();
	private static final Type WINDOW_TYPE = new TypeToken<WikiEnvelope<PriceWindow>>() {}.getType();
	private static final Type TIMESERIES_TYPE = new TypeToken<TimeseriesResponse>() {}.getType();
	private static final long HISTORY_CACHE_SECONDS = 15 * 60L;
	private static final int HISTORY_STAPLE_WARM_LIMIT = 12;
	private static final int HISTORY_OPPORTUNITY_WARM_LIMIT = 8;
	private static final int HISTORY_WARM_REQUEST_LIMIT = 18;
	private static final int HISTORY_VISIBLE_WARM_REQUEST_LIMIT = 18;
	private static final int HISTORY_WARM_CONCURRENCY = 6;
	private static final long TIMESERIES_CACHE_SECONDS = 5 * 60L;

	private final OkHttpClient okHttpClient;
	private final OkHttpClient historyHttpClient;
	private final Gson gson;
	private final RecommendationEngine recommendationEngine = new RecommendationEngine();
	private final HistoricalSignalAnalyzer historicalSignalAnalyzer = new HistoricalSignalAnalyzer();
	private final Map<Integer, CachedHistory> historyCache = Collections.synchronizedMap(new HashMap<>());
	private final Map<Integer, CachedTimeseries> timeseriesCache = Collections.synchronizedMap(new HashMap<>());

	@Inject
	WikiMarketClient(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.historyHttpClient = okHttpClient.newBuilder()
			.callTimeout(5, TimeUnit.SECONDS)
			.connectTimeout(4, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.build();
		this.gson = gson;
	}

	public SignalResponse getMarketSignals(MerchLensConfig config) throws IOException
	{
		return getMarketSignals(config, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new FlipHistorySummary(0, 0, 0, 0, 0, 0), new ArrayList<>());
	}

	public SignalResponse getMarketSignals(MerchLensConfig config, Collection<OfferSnapshot> offerSnapshots) throws IOException
	{
		return getMarketSignals(config, offerSnapshots, new ArrayList<>(), new ArrayList<>(), new FlipHistorySummary(0, 0, 0, 0, 0, 0), new ArrayList<>());
	}

	public SignalResponse getMarketSignals(
		MerchLensConfig config,
		Collection<OfferSnapshot> offerSnapshots,
		Collection<FlipRecord> openFlips,
		Collection<Integer> favoriteItemIds,
		FlipHistorySummary historySummary,
		List<FlipRecord> recentClosedFlips) throws IOException
	{
		Map<Integer, ItemMetadata> mapping = mapping();
		WikiEnvelope<LatestPrice> latest = getEnvelope("/latest", LATEST_TYPE);
		WikiEnvelope<PriceWindow> fiveMinute = getEnvelope("/5m", WINDOW_TYPE);
		WikiEnvelope<PriceWindow> oneHour = getEnvelope("/1h", WINDOW_TYPE);

		List<MarketItem> marketItems = new ArrayList<>();
		for (Map.Entry<Integer, LatestPrice> entry : latest.getData().entrySet())
		{
			ItemMetadata metadata = mapping.get(entry.getKey());
			if (metadata == null)
			{
				continue;
			}

			marketItems.add(new MarketItem(
				metadata,
				entry.getValue(),
				fiveMinute.getData().get(entry.getKey()),
				oneHour.getData().get(entry.getKey())
			));
		}

		List<RecommendationDto> initialStaples = recommendationEngine.highVolumeStaples(marketItems, config);
		List<RecommendationDto> initialOpportunities = recommendationEngine.screener(marketItems, config);
		Map<Integer, HistoricalSignal> history = historicalSignals(initialStaples, initialOpportunities, openFlips, favoriteItemIds, false);
		List<RecommendationDto> staples = recommendationEngine.highVolumeStaples(marketItems, config, history);
		List<RecommendationDto> opportunities = recommendationEngine.screener(marketItems, config, history);
		List<RecommendationDto> favorites = favoriteRecommendations(marketItems, config, history, favoriteItemIds);

		return new SignalResponse(
			System.currentTimeMillis() / 1000L,
			opportunities,
			staples,
			favorites,
			new LinkedHashSet<>(favoriteItemIds),
			offerAdvice(offerSnapshots, openFlips, mapping, latest.getData()),
			historySummary,
			enrichNames(recentClosedFlips, mapping)
		);
	}

	public int warmHistoricalSignals(List<RecommendationDto> staples, List<RecommendationDto> opportunities) throws IOException
	{
		return warmHistoricalSignals(staples, opportunities, new ArrayList<>());
	}

	public int warmHistoricalSignals(List<RecommendationDto> staples, List<RecommendationDto> opportunities, Collection<RecommendationDto> favorites) throws IOException
	{
		List<RecommendationDto> opportunitiesWithFavorites = new ArrayList<>(opportunities);
		List<Integer> favoriteItemIds = new ArrayList<>();
		for (RecommendationDto favorite : favorites)
		{
			opportunitiesWithFavorites.add(favorite);
			favoriteItemIds.add(favorite.getItemId());
		}
		return historicalSignals(staples, opportunitiesWithFavorites, new ArrayList<>(), favoriteItemIds, true).size();
	}

	public int warmHistoricalSignals(Collection<RecommendationDto> visibleRecommendations) throws IOException
	{
		Map<Integer, RecommendationDto> byId = new HashMap<>();
		Set<Integer> ids = new LinkedHashSet<>();
		for (RecommendationDto recommendation : visibleRecommendations)
		{
			if (recommendation == null || recommendation.getItemId() <= 0)
			{
				continue;
			}
			byId.putIfAbsent(recommendation.getItemId(), recommendation);
			ids.add(recommendation.getItemId());
			if (ids.size() >= HISTORY_VISIBLE_WARM_REQUEST_LIMIT)
			{
				break;
			}
		}
		return fetchHistoricalSignals(ids, byId).size();
	}

	public RecommendationDto searchItem(String query, MerchLensConfig config) throws IOException
	{
		if (query == null || query.trim().isEmpty())
		{
			throw new IOException("Enter an item name or item ID.");
		}

		Map<Integer, ItemMetadata> mapping = mapping();
		ItemMetadata metadata = findItem(mapping.values(), query.trim());
		WikiEnvelope<LatestPrice> latest = getEnvelope("/latest", LATEST_TYPE);
		LatestPrice latestPrice = latest.getData().get(metadata.getId());
		if (latestPrice == null || latestPrice.getHigh() == null || latestPrice.getLow() == null)
		{
			throw new IOException("No recent Wiki price data for " + metadata.getName() + ".");
		}

		WikiEnvelope<PriceWindow> fiveMinute = getEnvelope("/5m", WINDOW_TYPE);
		WikiEnvelope<PriceWindow> oneHour = getEnvelope("/1h", WINDOW_TYPE);
		MarketItem item = new MarketItem(
			metadata,
			latestPrice,
			fiveMinute.getData().get(metadata.getId()),
			oneHour.getData().get(metadata.getId())
		);
		HistoricalSignal history = historicalSignal(metadata.getId(), latestPrice.getLow(), latestPrice.getHigh(), true);
		RecommendationDto recommendation = recommendationEngine.inspect(item, config, history);
		if (recommendation == null)
		{
			throw new IOException("Unable to inspect " + metadata.getName() + ".");
		}
		return recommendation;
	}

	public List<ItemSearchResult> searchIndex() throws IOException
	{
		List<ItemSearchResult> results = new ArrayList<>();
		for (ItemMetadata item : mapping().values())
		{
			if (item.getName() != null && !item.getName().trim().isEmpty())
			{
				results.add(new ItemSearchResult(item.getId(), item.getName()));
			}
		}
		results.sort(Comparator.comparing(result -> result.getItemName().toLowerCase()));
		return results;
	}

	public List<TimeseriesPoint> dailyTimeseries(int itemId) throws IOException
	{
		long now = System.currentTimeMillis() / 1000L;
		CachedTimeseries cached = timeseriesCache.get(itemId);
		if (cached != null && now - cached.loadedAt <= TIMESERIES_CACHE_SECONDS)
		{
			return cached.points;
		}

		TimeseriesResponse response = timeseries(itemId);
		List<TimeseriesPoint> points = response == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(response.getData()));
		timeseriesCache.put(itemId, new CachedTimeseries(now, points));
		return points;
	}

	private Map<Integer, HistoricalSignal> historicalSignals(
		List<RecommendationDto> staples,
		List<RecommendationDto> opportunities,
		Collection<FlipRecord> openFlips,
		Collection<Integer> favoriteItemIds,
		boolean fetchMissing) throws IOException
	{
		Map<Integer, HistoricalSignal> signals = new HashMap<>();
		Set<Integer> ids = new LinkedHashSet<>();
		Map<Integer, RecommendationDto> byId = new HashMap<>();
		staples.forEach(rec -> byId.put(rec.getItemId(), rec));
		opportunities.forEach(rec -> byId.putIfAbsent(rec.getItemId(), rec));
		if (fetchMissing)
		{
			addFavoriteIds(ids, favoriteItemIds);
			addRecommendationIds(ids, sortedByLimitProfit(staples), HISTORY_STAPLE_WARM_LIMIT);
			addRecommendationIds(ids, opportunities, HISTORY_OPPORTUNITY_WARM_LIMIT);
			addRecommendationIds(ids, staples, 8);
			addRecommendationIds(ids, sortedByBuyPrice(staples), 5);
			addRecommendationIds(ids, sortedByNetMargin(staples), 5);
			addRecommendationIds(ids, sortedByHourlyVolume(staples), 5);
			trimIds(ids, HISTORY_WARM_REQUEST_LIMIT);
		}
		else
		{
			addRecommendationIds(ids, staples, 100);
			addRecommendationIds(ids, opportunities, opportunities.size());
		}
		for (FlipRecord record : openFlips)
		{
			ids.add(record.getItemId());
		}
		addFavoriteIds(ids, favoriteItemIds);
		if (fetchMissing)
		{
			return fetchHistoricalSignals(ids, byId);
		}
		for (Integer itemId : ids)
		{
			RecommendationDto recommendation = byId.get(itemId);
			if (recommendation == null)
			{
				HistoricalSignal cached = historicalSignal(itemId, 0, 0, false);
				if (cached != null)
				{
					signals.put(itemId, cached);
				}
				continue;
			}
			HistoricalSignal signal = historicalSignal(itemId, recommendation.getBuyPrice(), recommendation.getSellPrice(), fetchMissing);
			if (signal != null)
			{
				signals.put(itemId, signal);
			}
		}
		return signals;
	}

	private void addFavoriteIds(Set<Integer> ids, Collection<Integer> favoriteItemIds)
	{
		for (Integer itemId : favoriteItemIds)
		{
			if (itemId != null && itemId > 0)
			{
				ids.add(itemId);
			}
		}
	}

	private void addRecommendationIds(Set<Integer> ids, List<RecommendationDto> recommendations, int limit)
	{
		recommendations.stream()
			.limit(limit)
			.forEach(rec -> ids.add(rec.getItemId()));
	}

	private void trimIds(Set<Integer> ids, int limit)
	{
		while (ids.size() > limit)
		{
			Integer last = null;
			for (Integer id : ids)
			{
				last = id;
			}
			if (last == null)
			{
				return;
			}
			ids.remove(last);
		}
	}

	private List<RecommendationDto> sortedByLimitProfit(List<RecommendationDto> recommendations)
	{
		List<RecommendationDto> sorted = new ArrayList<>(recommendations);
		sorted.sort(Comparator
			.comparingLong((RecommendationDto rec) -> (long) rec.getNetMargin() * rec.getBuyLimit()).reversed()
			.thenComparing(Comparator.comparingDouble(RecommendationDto::getRoi).reversed()));
		return sorted;
	}

	private List<RecommendationDto> sortedByHourlyVolume(List<RecommendationDto> recommendations)
	{
		List<RecommendationDto> sorted = new ArrayList<>(recommendations);
		sorted.sort(Comparator
			.comparingInt(RecommendationDto::getHourlyVolume).reversed()
			.thenComparing(Comparator.comparingLong((RecommendationDto rec) -> (long) rec.getNetMargin() * rec.getBuyLimit()).reversed()));
		return sorted;
	}

	private List<RecommendationDto> sortedByBuyPrice(List<RecommendationDto> recommendations)
	{
		List<RecommendationDto> sorted = new ArrayList<>(recommendations);
		sorted.sort(Comparator
			.comparingInt(RecommendationDto::getBuyPrice)
			.thenComparing(Comparator.comparingLong((RecommendationDto rec) -> (long) rec.getNetMargin() * rec.getBuyLimit()).reversed()));
		return sorted;
	}

	private List<RecommendationDto> sortedByNetMargin(List<RecommendationDto> recommendations)
	{
		List<RecommendationDto> sorted = new ArrayList<>(recommendations);
		sorted.sort(Comparator
			.comparingInt(RecommendationDto::getNetMargin).reversed()
			.thenComparing(Comparator.comparingLong((RecommendationDto rec) -> (long) rec.getNetMargin() * rec.getBuyLimit()).reversed()));
		return sorted;
	}

	private Map<Integer, HistoricalSignal> fetchHistoricalSignals(Set<Integer> ids, Map<Integer, RecommendationDto> byId) throws IOException
	{
		Map<Integer, HistoricalSignal> signals = new HashMap<>();
		if (ids.isEmpty())
		{
			return signals;
		}

		ExecutorService executor = Executors.newFixedThreadPool(Math.min(HISTORY_WARM_CONCURRENCY, ids.size()), runnable -> {
			Thread thread = new Thread(runnable, "merch-lens-trend");
			thread.setDaemon(true);
			return thread;
		});
		try
		{
			List<Callable<HistoryResult>> tasks = new ArrayList<>();
			List<Integer> taskItemIds = new ArrayList<>();
			for (Integer itemId : ids)
			{
				RecommendationDto recommendation = byId.get(itemId);
				if (recommendation == null)
				{
					continue;
				}
				tasks.add(() -> fetchHistoricalSignal(itemId, recommendation));
				taskItemIds.add(itemId);
			}
			List<Future<HistoryResult>> futures = executor.invokeAll(tasks);
			for (int i = 0; i < futures.size(); i++)
			{
				Future<HistoryResult> future = futures.get(i);
				Integer itemId = taskItemIds.get(i);
				if (future.isCancelled())
				{
					signals.put(itemId, cacheUnavailableHistorySignal(itemId));
					continue;
				}
				try
				{
					HistoryResult result = future.get();
					if (result.signal != null)
					{
						signals.put(result.itemId, result.signal);
					}
					else
					{
						signals.put(itemId, cacheUnavailableHistorySignal(itemId));
					}
				}
				catch (ExecutionException ignored)
				{
					signals.put(itemId, cacheUnavailableHistorySignal(itemId));
				}
			}
		}
		catch (InterruptedException ex)
		{
			throw new IOException("Unable to finish trend data warmup", ex);
		}
		finally
		{
			executor.shutdown();
		}
		return signals;
	}

	private HistoryResult fetchHistoricalSignal(Integer itemId, RecommendationDto recommendation)
	{
		try
		{
			HistoricalSignal signal = historicalSignal(itemId, recommendation.getBuyPrice(), recommendation.getSellPrice(), true);
			return new HistoryResult(itemId, signal == null ? cacheUnavailableHistorySignal(itemId) : signal);
		}
		catch (Exception ex)
		{
			return new HistoryResult(itemId, cacheUnavailableHistorySignal(itemId));
		}
	}

	private List<RecommendationDto> favoriteRecommendations(
		List<MarketItem> items,
		MerchLensConfig config,
		Map<Integer, HistoricalSignal> history,
		Collection<Integer> favoriteItemIds)
	{
		Map<Integer, MarketItem> byId = new HashMap<>();
		for (MarketItem item : items)
		{
			byId.put(item.getMetadata().getId(), item);
		}

		List<RecommendationDto> favorites = new ArrayList<>();
		for (Integer itemId : favoriteItemIds)
		{
			if (itemId == null)
			{
				continue;
			}
			MarketItem item = byId.get(itemId);
			if (item == null)
			{
				continue;
			}
			if (item.getLatestPrice().getLow() == null || item.getLatestPrice().getLow() > config.budget())
			{
				continue;
			}
			RecommendationDto recommendation = recommendationEngine.inspect(item, config, history.get(itemId));
			if (recommendation != null)
			{
				favorites.add(recommendation);
			}
		}
		return favorites;
	}

	private HistoricalSignal historicalSignal(int itemId, int currentLow, int currentHigh, boolean fetchMissing) throws IOException
	{
		long now = System.currentTimeMillis() / 1000L;
		CachedHistory cached = historyCache.get(itemId);
		if (cached != null && now - cached.loadedAt <= HISTORY_CACHE_SECONDS)
		{
			return cached.signal;
		}
		if (!fetchMissing)
		{
			return null;
		}
		List<TimeseriesPoint> points = dailyTimeseries(itemId);
		if (points.isEmpty())
		{
			return cacheUnavailableHistorySignal(itemId);
		}
		HistoricalSignal signal = historicalSignalAnalyzer.analyze(itemId, currentLow, currentHigh, points);
		if (signal != null)
		{
			historyCache.put(itemId, new CachedHistory(now, signal));
		}
		return signal;
	}

	private HistoricalSignal cacheUnavailableHistorySignal(int itemId)
	{
		HistoricalSignal unavailable = unavailableHistorySignal(itemId);
		historyCache.put(itemId, new CachedHistory(System.currentTimeMillis() / 1000L, unavailable));
		return unavailable;
	}

	private TimeseriesResponse timeseries(int itemId) throws IOException
	{
		HttpUrl base = HttpUrl.parse(WIKI_BASE);
		if (base == null)
		{
			throw new IOException("Invalid OSRS Wiki API URL");
		}
		HttpUrl url = base.newBuilder()
			.addPathSegment("timeseries")
			.addQueryParameter("timestep", "5m")
			.addQueryParameter("id", Integer.toString(itemId))
			.build();
		return gson.fromJson(get(url, historyHttpClient), TIMESERIES_TYPE);
	}

	private HistoricalSignal unavailableHistorySignal(int itemId)
	{
		return new HistoricalSignal(
			itemId,
			HistoricalSignal.MarketState.INSUFFICIENT_HISTORY,
			0,
			0,
			0,
			1,
			0,
			1,
			"Trend data was unavailable or timed out. Refresh later."
		);
	}

	private ItemMetadata findItem(Collection<ItemMetadata> items, String query) throws IOException
	{
		try
		{
			int itemId = Integer.parseInt(query);
			for (ItemMetadata item : items)
			{
				if (item.getId() == itemId)
				{
					return item;
				}
			}
		}
		catch (NumberFormatException ignored)
		{
			// Fall through to name matching.
		}

		String normalized = query.toLowerCase();
		ItemMetadata startsWith = null;
		ItemMetadata contains = null;
		for (ItemMetadata item : items)
		{
			String name = item.getName();
			if (name == null)
			{
				continue;
			}
			String lower = name.toLowerCase();
			if (lower.equals(normalized))
			{
				return item;
			}
			if (startsWith == null && lower.startsWith(normalized))
			{
				startsWith = item;
			}
			if (contains == null && lower.contains(normalized))
			{
				contains = item;
			}
		}
		if (startsWith != null)
		{
			return startsWith;
		}
		if (contains != null)
		{
			return contains;
		}
		throw new IOException("No item matched \"" + query + "\".");
	}

	private List<FlipRecord> enrichNames(List<FlipRecord> records, Map<Integer, ItemMetadata> mapping)
	{
		for (FlipRecord record : records)
		{
			ItemMetadata metadata = mapping.get(record.getItemId());
			if (metadata != null)
			{
				record.setItemName(metadata.getName());
			}
		}
		return records;
	}

	private List<OfferAdvice> offerAdvice(
		Collection<OfferSnapshot> offerSnapshots,
		Collection<FlipRecord> openFlips,
		Map<Integer, ItemMetadata> mapping,
		Map<Integer, LatestPrice> latest)
	{
		Map<Integer, OfferSnapshot> latestByItem = new HashMap<>();
		for (OfferSnapshot snapshot : offerSnapshots)
		{
			if (snapshot.getItemId() <= 0)
			{
				continue;
			}
			OfferSnapshot previous = latestByItem.get(snapshot.getItemId());
			if (previous == null || snapshot.getUpdatedAt() >= previous.getUpdatedAt())
			{
				latestByItem.put(snapshot.getItemId(), snapshot);
			}
		}

		List<OfferAdvice> advice = new ArrayList<>();
		for (OfferSnapshot snapshot : latestByItem.values())
		{
			ItemMetadata metadata = mapping.get(snapshot.getItemId());
			LatestPrice price = latest.get(snapshot.getItemId());
			if (metadata == null || price == null || price.getHigh() == null)
			{
				continue;
			}
			OfferAdvice itemAdvice = offerAdvice(snapshot, metadata, price.getHigh());
			if (itemAdvice != null)
			{
				advice.add(itemAdvice);
			}
		}
		for (FlipRecord record : openFlips)
		{
			if (latestByItem.containsKey(record.getItemId()))
			{
				continue;
			}
			ItemMetadata metadata = mapping.get(record.getItemId());
			LatestPrice price = latest.get(record.getItemId());
			if (metadata == null || price == null || price.getHigh() == null)
			{
				continue;
			}
			record.setItemName(metadata.getName());
			advice.add(offerAdvice(record, metadata, price.getHigh()));
		}
		return advice;
	}

	private OfferAdvice offerAdvice(OfferSnapshot snapshot, ItemMetadata metadata, int targetSellPrice)
	{
		String state = snapshot.getState();
		if (!"BOUGHT".equals(state) && !"SELLING".equals(state) && !"CANCELLED_SELL".equals(state))
		{
			return null;
		}

		int filled = Math.max(snapshot.getFilledQuantity(), 0);
		int averageBuyPrice = filled > 0 && snapshot.getSpent() > 0 ? snapshot.getSpent() / filled : snapshot.getPrice();
		int netMargin = GeTax.netMargin(averageBuyPrice, targetSellPrice, metadata.getName());
		String action;
		String note;
		if ("SELLING".equals(state))
		{
			int delta = targetSellPrice - snapshot.getPrice();
			if (Math.abs(delta) < Math.max(10, snapshot.getPrice() * 0.002))
			{
				action = "Hold sell offer";
				note = "Your sell price is close to the current Wiki high.";
			}
			else
			{
				action = delta > 0 ? "Consider higher relist" : "Consider lower relist";
				note = "Current Wiki high moved " + formatSigned(delta) + " gp from your sell offer.";
			}
		}
		else
		{
			action = netMargin > 0 ? "Sell target updated" : "Review before selling";
			note = netMargin > 0
				? "Use fresh market data before listing; target is based on latest Wiki high."
				: "Current target would be tax-negative versus the observed buy price.";
		}

		return new OfferAdvice(
			snapshot.getItemId(),
			null,
			metadata.getName(),
			state,
			action,
			snapshot.getPrice(),
			filled,
			averageBuyPrice,
			targetSellPrice,
			netMargin,
			note
		);
	}

	private OfferAdvice offerAdvice(FlipRecord record, ItemMetadata metadata, int targetSellPrice)
	{
		int averageBuyPrice = record.getAverageBuyPrice();
		int netMargin = GeTax.netMargin(averageBuyPrice, targetSellPrice, metadata.getName());
		String action = netMargin > 0 ? "Open flip sell target" : "Open flip needs review";
		String note = netMargin > 0
			? "Fresh target from latest Wiki high. Refresh again before listing if this buy is old."
			: "Current market target would be tax-negative versus your recorded buy.";
		return new OfferAdvice(
			record.getItemId(),
			record.getId(),
			metadata.getName(),
			"OPEN",
			action,
			record.getBuyPrice(),
			record.getBuyQuantity(),
			averageBuyPrice,
			targetSellPrice,
			netMargin,
			note
		);
	}

	private String formatSigned(int value)
	{
		return value >= 0 ? "+" + value : Integer.toString(value);
	}

	private Map<Integer, ItemMetadata> mapping() throws IOException
	{
		ItemMetadata[] items = gson.fromJson(get("/mapping"), ItemMetadata[].class);
		Map<Integer, ItemMetadata> byId = new HashMap<>();
		for (ItemMetadata item : items)
		{
			byId.put(item.getId(), item);
		}
		return byId;
	}

	private <T> WikiEnvelope<T> getEnvelope(String path, Type type) throws IOException
	{
		return gson.fromJson(get(path), type);
	}

	private String get(String path) throws IOException
	{
		HttpUrl base = HttpUrl.parse(WIKI_BASE);
		if (base == null)
		{
			throw new IOException("Invalid OSRS Wiki API URL");
		}

		HttpUrl url = base.newBuilder()
			.addPathSegments(path.substring(1))
			.build();
		return get(url);
	}

	private String get(HttpUrl url) throws IOException
	{
		return get(url, okHttpClient);
	}

	private String get(HttpUrl url, OkHttpClient client) throws IOException
	{
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", "MerchLensRuneLite/0.1")
			.get()
			.build();

		try (Response response = client.newCall(request).execute())
		{
			if (response.code() != HttpURLConnection.HTTP_OK)
			{
				throw new IOException("OSRS Wiki API returned HTTP " + response.code());
			}

			ResponseBody body = response.body();
			if (body == null)
			{
				throw new IOException("OSRS Wiki API returned an empty response");
			}

			return body.string();
		}
	}

	private static class CachedHistory
	{
		private final long loadedAt;
		private final HistoricalSignal signal;

		private CachedHistory(long loadedAt, HistoricalSignal signal)
		{
			this.loadedAt = loadedAt;
			this.signal = signal;
		}
	}

	private static class CachedTimeseries
	{
		private final long loadedAt;
		private final List<TimeseriesPoint> points;

		private CachedTimeseries(long loadedAt, List<TimeseriesPoint> points)
		{
			this.loadedAt = loadedAt;
			this.points = points;
		}
	}

	private static class HistoryResult
	{
		private final int itemId;
		private final HistoricalSignal signal;

		private HistoryResult(int itemId, HistoricalSignal signal)
		{
			this.itemId = itemId;
			this.signal = signal;
		}
	}
}
