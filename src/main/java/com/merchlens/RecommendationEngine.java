package com.merchlens;

import com.merchlens.model.MarketItem;
import com.merchlens.model.RecommendationDto;
import com.merchlens.model.HistoricalSignal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class RecommendationEngine
{
	private static final int ACTIONABLE_PRICE_AGE_MINUTES = 10;
	private static final int MAX_STAPLE_PRICE_AGE_MINUTES = 60;
	private static final int STRATEGY_HOLD_WINDOW_HOURS = 12;
	private static final double STRATEGY_MINIMUM_ROI = 0.0025;
	private static final int SAFE_MIN_HOURLY_VOLUME = 250;
	private static final int HIGH_VOLUME_STAPLE_LIMIT = 350;

	List<RecommendationDto> screener(List<MarketItem> items, MerchLensConfig config)
	{
		return screener(items, config, new LinkedHashMap<>());
	}

	List<RecommendationDto> screener(List<MarketItem> items, MerchLensConfig config, Map<Integer, HistoricalSignal> history)
	{
		List<RecommendationDto> recommendations = new ArrayList<>();
		long now = System.currentTimeMillis() / 1000L;
		for (MarketItem item : items)
		{
			RecommendationDto recommendation = evaluateScreener(item, config, now, history.get(item.getMetadata().getId()));
			if (recommendation != null)
			{
				recommendations.add(recommendation);
			}
		}

		recommendations.sort(Comparator
			.comparingLong((RecommendationDto rec) -> Math.max(0L, rec.getFourHourFlowProfit())).reversed()
			.thenComparing(Comparator.comparingLong((RecommendationDto rec) -> Math.max(0L, (long) rec.getNetMargin() * rec.getBuyLimit())).reversed())
			.thenComparing(Comparator.comparingDouble(RecommendationDto::getRoi).reversed())
			.thenComparing(Comparator.comparingInt(RecommendationDto::getHourlyVolume).reversed()));
		return recommendations;
	}

	List<RecommendationDto> highVolumeStaples(List<MarketItem> items, MerchLensConfig config)
	{
		return highVolumeStaples(items, config, new LinkedHashMap<>());
	}

	List<RecommendationDto> highVolumeStaples(List<MarketItem> items, MerchLensConfig config, Map<Integer, HistoricalSignal> history)
	{
		List<RecommendationDto> recommendations = new ArrayList<>();
		long now = System.currentTimeMillis() / 1000L;
		for (MarketItem item : items)
		{
			if (!isHighVolumeStaple(item))
			{
				continue;
			}
			RecommendationDto recommendation = evaluateStaple(item, config, now, history.get(item.getMetadata().getId()));
			if (recommendation != null)
			{
				recommendations.add(recommendation);
			}
		}
		recommendations.sort(Comparator
			.comparing(RecommendationDto::getRoi, Comparator.reverseOrder())
			.thenComparing(RecommendationDto::getHourlyVolume, Comparator.reverseOrder()));
		return recommendations.subList(0, Math.min(HIGH_VOLUME_STAPLE_LIMIT, recommendations.size()));
	}

	RecommendationDto inspect(MarketItem item, MerchLensConfig config, HistoricalSignal historicalSignal)
	{
		long now = System.currentTimeMillis() / 1000L;
		if (item.getLatestPrice().getHigh() == null || item.getLatestPrice().getLow() == null)
		{
			return null;
		}

		String name = item.getMetadata().getName();
		int buyPrice = item.getLatestPrice().getLow();
		int sellPrice = item.getLatestPrice().getHigh();
		int tax = GeTax.tax(sellPrice, name);
		int netMargin = GeTax.netMargin(buyPrice, sellPrice, name);
		double roi = buyPrice > 0 ? (double) netMargin / buyPrice : 0;
		int quantity = Math.max(1, item.getMetadata().getLimit());
		int expectedProfit = netMargin * quantity;
		int capital = buyPrice * quantity;
		Map<String, Double> confidence = confidence(item, config, now, netMargin, roi, quantity, capital);
		double confidenceScore = confidence.get("score");
		double fillProbability = confidence.get("fillProbability");
		int latestAgeMinutes = latestAgeMinutes(item, now);
		String strategy = strategy(item, netMargin, roi, confidence);
		String risk = risk(strategy, confidenceScore);
		String action = netMargin > 0 ? "Watch" : "Avoid";
		List<String> warnings = warnings(item, now, netMargin, roi, confidence, strategy);

		if (historicalSignal != null && !isHistoricallyActionable(historicalSignal))
		{
			warnings.add(historicalSignal.getWarning());
		}
		else if (netMargin > 0 && latestAgeMinutes <= ACTIONABLE_PRICE_AGE_MINUTES)
		{
			action = "Buy";
		}
		if (netMargin <= 0)
		{
			strategy = "Avoid/Trap Detection";
			risk = "ADVANCED";
		}

		double expectedHold = Math.max(0.5, STRATEGY_HOLD_WINDOW_HOURS * (1.2 - fillProbability * 0.4));
		double score = "Avoid".equals(action) ? 0 : confidenceScore * Math.max(expectedProfit, 0) * fillProbability;
		String explanation = explanation(item, action, strategy, netMargin, roi, quantity, confidence, warnings);

		return new RecommendationDto(
			item.getMetadata().getId(),
			name,
			action,
			strategy,
			risk,
			buyPrice,
			sellPrice,
			quantity,
			item.getMetadata().getLimit(),
			tax,
			netMargin,
			expectedProfit,
			roi,
			capital,
			expectedHold,
			expectedProfit / Math.max(expectedHold, 0.25),
			expectedProfit / (double) Math.max(capital, 1) / Math.max(expectedHold, 0.25),
			score,
			hourlyVolume(item),
			lowSideVolume(item),
			highSideVolume(item),
			latestAgeMinutes,
			marketState(historicalSignal),
			marketWarning(historicalSignal),
			explanation,
			warnings,
			confidence
		);
	}

	private RecommendationDto evaluateStaple(MarketItem item, MerchLensConfig config, long now, HistoricalSignal historicalSignal)
	{
		if (item.getLatestPrice().getHigh() == null || item.getLatestPrice().getLow() == null)
		{
			return null;
		}
		int hourlyVolume = hourlyVolume(item);
		if (hourlyVolume < 1_000 || lowSideVolume(item) < 200 || highSideVolume(item) < 200)
		{
			return null;
		}
		int latestAgeMinutes = latestAgeMinutes(item, now);
		if (latestAgeMinutes > MAX_STAPLE_PRICE_AGE_MINUTES)
		{
			return null;
		}

		String name = item.getMetadata().getName();
		int buyPrice = item.getLatestPrice().getLow();
		int sellPrice = item.getLatestPrice().getHigh();
		if (buyPrice <= 0)
		{
			return null;
		}
		int tax = GeTax.tax(sellPrice, name);
		int netMargin = GeTax.netMargin(buyPrice, sellPrice, name);
		double roi = buyPrice > 0 ? (double) netMargin / buyPrice : 0;
		int buyLimit = Math.max(1, item.getMetadata().getLimit());
		int expectedProfit = clampedInt((long) netMargin * buyLimit);
		int capital = clampedInt((long) buyPrice * buyLimit);
		Map<String, Double> confidence = confidence(item, config, now, netMargin, roi, buyLimit, capital);
		boolean historicallyActionable = historicalSignal == null || isHistoricallyActionable(historicalSignal);
		String action = netMargin > 0 && latestAgeMinutes <= ACTIONABLE_PRICE_AGE_MINUTES && historicallyActionable ? "Buy" : "Watch";
		String explanation = netMargin > 0
			? "High-volume staple with a current post-tax spread. Still verify the live GE margin before committing."
			: "High-volume staple worth watching, but the current post-tax spread is not positive.";
		if (latestAgeMinutes > ACTIONABLE_PRICE_AGE_MINUTES)
		{
			explanation = "High-volume staple, but the latest Wiki buy/sell prices are stale. Refresh or manually margin-check before trading.";
		}
		if (historicalSignal != null && !historicallyActionable)
		{
			explanation = historicalSignal.getWarning();
		}

		return new RecommendationDto(
			item.getMetadata().getId(),
			name,
			action,
			"High Volume Staple",
			"SAFE",
			buyPrice,
			sellPrice,
			buyLimit,
			item.getMetadata().getLimit(),
			tax,
			netMargin,
			expectedProfit,
			roi,
			capital,
			Math.max(0.5, STRATEGY_HOLD_WINDOW_HOURS * 0.75),
			expectedProfit / Math.max(STRATEGY_HOLD_WINDOW_HOURS * 0.75, 0.25),
			expectedProfit / (double) Math.max(capital, 1) / Math.max(STRATEGY_HOLD_WINDOW_HOURS * 0.75, 0.25),
			confidence.get("score"),
			hourlyVolume,
			lowSideVolume(item),
			highSideVolume(item),
			latestAgeMinutes,
			marketState(historicalSignal),
			marketWarning(historicalSignal),
			explanation,
			warnings(item, now, netMargin, roi, confidence, "High Volume Staple"),
			confidence
		);
	}

	private RecommendationDto evaluateScreener(MarketItem item, MerchLensConfig config, long now, HistoricalSignal historicalSignal)
	{
		if (item.getLatestPrice().getHigh() == null || item.getLatestPrice().getLow() == null)
		{
			return null;
		}

		String name = item.getMetadata().getName();
		int buyPrice = item.getLatestPrice().getLow();
		int sellPrice = item.getLatestPrice().getHigh();
		if (buyPrice <= 0)
		{
			return null;
		}

		int buyLimit = Math.max(1, item.getMetadata().getLimit());
		int tax = GeTax.tax(sellPrice, name);
		int netMargin = GeTax.netMargin(buyPrice, sellPrice, name);
		double roi = buyPrice > 0 ? (double) netMargin / buyPrice : 0;
		int expectedProfit = clampedInt((long) netMargin * buyLimit);
		int capital = clampedInt((long) buyPrice * buyLimit);
		Map<String, Double> confidence = confidence(item, config, now, netMargin, roi, buyLimit, capital);
		int latestAgeMinutes = latestAgeMinutes(item, now);
		boolean historicallyActionable = historicalSignal == null || isHistoricallyActionable(historicalSignal);
		String action = netMargin > 0 && latestAgeMinutes <= ACTIONABLE_PRICE_AGE_MINUTES && historicallyActionable ? "Buy" : "Watch";
		if (netMargin <= 0)
		{
			action = "Avoid";
		}
		double expectedHold = Math.max(0.5, STRATEGY_HOLD_WINDOW_HOURS * 0.75);
		double score = Math.max(0, expectedProfit) * Math.max(0.01, confidence.get("score"));
		List<String> warnings = warnings(item, now, netMargin, roi, confidence, "Market Screener");
		if (historicalSignal != null && !historicallyActionable)
		{
			warnings.add(historicalSignal.getWarning());
		}

		return new RecommendationDto(
			item.getMetadata().getId(),
			name,
			action,
			"Market Screener",
			netMargin > 0 ? "BALANCED" : "ADVANCED",
			buyPrice,
			sellPrice,
			buyLimit,
			buyLimit,
			tax,
			netMargin,
			expectedProfit,
			roi,
			capital,
			expectedHold,
			expectedProfit / Math.max(expectedHold, 0.25),
			expectedProfit / (double) Math.max(capital, 1) / Math.max(expectedHold, 0.25),
			score,
			hourlyVolume(item),
			lowSideVolume(item),
			highSideVolume(item),
			latestAgeMinutes,
			marketState(historicalSignal),
			marketWarning(historicalSignal),
			"Market screener item. Review the card metrics and verify the live GE margin before trading.",
			warnings,
			confidence
		);
	}

	private Map<String, Double> confidence(MarketItem item, MerchLensConfig config, long now, int margin, double roi, int quantity, int capital)
	{
		double spreadQuality = margin <= 0 ? 0 : Math.min(1, (margin / Math.max(config.minimumProfit() / 20.0, 1)) * 0.45 + (roi / Math.max(STRATEGY_MINIMUM_ROI * 3, 0.0001)) * 0.55);
		double liquidity = liquidity(item, quantity);
		double stability = stability(item);
		double rangeReliability = rangeReliability(item);
		double freshness = freshness(item, now);
		double buyLimitFit = quantity <= 0 ? 0 : Math.min(1, 0.45 + (quantity / (double) item.getMetadata().getLimit()));
		double fillProbability = Math.min(1, liquidity * 0.4 + stability * 0.25 + freshness * 0.2 + rangeReliability * 0.15);
		double score = geometricMean(spreadQuality, liquidity, stability, rangeReliability, freshness, buyLimitFit, fillProbability);

		Map<String, Double> result = new LinkedHashMap<>();
		result.put("spreadQuality", spreadQuality);
		result.put("liquidity", liquidity);
		result.put("stability", stability);
		result.put("rangeReliability", rangeReliability);
		result.put("freshness", freshness);
		result.put("buyLimitFit", buyLimitFit);
		result.put("fillProbability", fillProbability);
		result.put("userFit", 1.0);
		result.put("score", score);
		return result;
	}

	private double liquidity(MarketItem item, int quantity)
	{
		int hourlyVolume = hourlyVolume(item);
		if (hourlyVolume <= 0)
		{
			return 0;
		}
		double relativeFill = Math.min(1, (hourlyVolume / (double) Math.max(quantity, 1)) / 8);
		double absoluteVolume = Math.min(1, hourlyVolume / 2_000.0);
		double sideBalance = sideBalance(item);
		return relativeFill * 0.45 + absoluteVolume * 0.35 + sideBalance * 0.20;
	}

	private double stability(MarketItem item)
	{
		if (item.getOneHour() == null || item.getOneHour().getAvgHighPrice() == null || item.getOneHour().getAvgLowPrice() == null)
		{
			return 0.45;
		}
		int liveSpread = Math.max(1, item.getLatestPrice().getHigh() - item.getLatestPrice().getLow());
		int hourlySpread = Math.max(1, item.getOneHour().getAvgHighPrice() - item.getOneHour().getAvgLowPrice());
		double expansion = Math.abs(liveSpread - hourlySpread) / (double) Math.max(hourlySpread, 1);
		return Math.max(0.1, Math.min(1, 1 - expansion / 2));
	}

	private double rangeReliability(MarketItem item)
	{
		if (item.getOneHour() == null || item.getOneHour().getAvgHighPrice() == null || item.getOneHour().getAvgLowPrice() == null)
		{
			return 0.35;
		}
		int currentMid = (item.getLatestPrice().getHigh() + item.getLatestPrice().getLow()) / 2;
		int hourlyMid = (item.getOneHour().getAvgHighPrice() + item.getOneHour().getAvgLowPrice()) / 2;
		double deviation = Math.abs(currentMid - hourlyMid) / (double) Math.max(hourlyMid, 1);
		return Math.max(0.2, Math.min(0.9, 1 - deviation / 0.08));
	}

	private double freshness(MarketItem item, long now)
	{
		Long highTime = item.getLatestPrice().getHighTime();
		Long lowTime = item.getLatestPrice().getLowTime();
		if (highTime == null || lowTime == null)
		{
			return 0;
		}
		double ageMinutes = (now - Math.min(highTime, lowTime)) / 60.0;
		if (ageMinutes <= 10)
		{
			return 1;
		}
		if (ageMinutes >= 120)
		{
			return 0.1;
		}
		return 1 - ((ageMinutes - 10) / 125);
	}

	private String strategy(MarketItem item, int margin, double roi, Map<String, Double> confidence)
	{
		int hourlyVolume = hourlyVolume(item);
		if (margin <= 0)
		{
			return "Avoid/Trap Detection";
		}
		if (hourlyVolume >= SAFE_MIN_HOURLY_VOLUME && confidence.get("rangeReliability") >= 0.7 && confidence.get("stability") >= 0.6)
		{
			return "Range-Bound Swing Flip";
		}
		if (roi >= 0.035 && hourlyVolume < item.getMetadata().getLimit() * 3)
		{
			return "High-Margin Gear Flip";
		}
		if (confidence.get("rangeReliability") >= 0.55 && confidence.get("stability") < 0.55)
		{
			return "Dip Recovery Flip";
		}
		return "Safe Passive Volume Flip";
	}

	private String risk(String strategy, double score)
	{
		if ("High-Margin Gear Flip".equals(strategy) || "Dip Recovery Flip".equals(strategy))
		{
			return score < 0.72 ? "ADVANCED" : "BALANCED";
		}
		return score >= 0.78 ? "SAFE" : "BALANCED";
	}

	private List<String> warnings(MarketItem item, long now, int margin, double roi, Map<String, Double> confidence, String strategy)
	{
		List<String> warnings = new ArrayList<>();
		if (margin <= 0)
		{
			warnings.add("Tax-negative or zero post-tax margin.");
		}
		if (confidence.get("freshness") < 0.45)
		{
			warnings.add("Price data is stale enough to be risky.");
		}
		if (latestAgeMinutes(item, now) > ACTIONABLE_PRICE_AGE_MINUTES)
		{
			warnings.add("Prices are stale; do not treat this as an immediate buy.");
		}
		if (confidence.get("liquidity") < 0.35)
		{
			warnings.add("Low volume may prevent one or both legs from filling.");
		}
		if (confidence.get("stability") < 0.35)
		{
			warnings.add("Recent price movement is too volatile for passive holding.");
		}
		if ("High-Margin Gear Flip".equals(strategy))
		{
			warnings.add("High-margin gear flips require more item knowledge and patience.");
		}
		if (roi < 0.0025)
		{
			warnings.add("ROI is thin after GE tax.");
		}
		return warnings;
	}

	private String explanation(MarketItem item, String action, String strategy, int margin, double roi, int quantity, Map<String, Double> confidence, List<String> warnings)
	{
		if ("Avoid".equals(action))
		{
			return "Avoid " + item.getMetadata().getName() + ": " + (warnings.isEmpty() ? "The evidence is not strong enough." : warnings.get(0));
		}
		int hourlyVolume = hourlyVolume(item);
		double coverage = hourlyVolume / (double) Math.max(quantity, 1);
		return action + " " + item.getMetadata().getName() + ": " + strategy + " with " + String.format("%,d", margin)
			+ " GP net margin (" + String.format("%.2f%%", roi * 100) + " ROI). Confidence is "
			+ String.format("%.0f%%", confidence.get("score") * 100) + " because 1h volume covers "
			+ String.format("%.1f", coverage) + "x the quantity, the spread survives 2% GE tax, and prices are fresh.";
	}

	private double geometricMean(double... values)
	{
		double product = 1;
		for (double value : values)
		{
			product *= Math.max(0, Math.min(1, value));
		}
		return Math.pow(product, 1.0 / values.length);
	}

	private int clampedInt(long value)
	{
		if (value > Integer.MAX_VALUE)
		{
			return Integer.MAX_VALUE;
		}
		if (value < Integer.MIN_VALUE)
		{
			return Integer.MIN_VALUE;
		}
		return (int) value;
	}

	private int hourlyVolume(MarketItem item)
	{
		int hourlyVolume = item.getOneHour() == null ? 0 : item.getOneHour().getTotalVolume();
		if (hourlyVolume <= 0 && item.getFiveMinute() != null)
		{
			hourlyVolume = item.getFiveMinute().getTotalVolume() * 12;
		}
		return hourlyVolume;
	}

	private int latestAgeMinutes(MarketItem item, long now)
	{
		Long highTime = item.getLatestPrice().getHighTime();
		Long lowTime = item.getLatestPrice().getLowTime();
		if (highTime == null || lowTime == null)
		{
			return Integer.MAX_VALUE;
		}
		return (int) Math.max(0, (now - Math.min(highTime, lowTime)) / 60);
	}

	private boolean isHighVolumeStaple(MarketItem item)
	{
		return HighVolumeItemCatalog.isCandidate(item.getMetadata().getName());
	}

	private int lowSideVolume(MarketItem item)
	{
		if (item.getOneHour() != null)
		{
			return item.getOneHour().getLowPriceVolume();
		}
		return item.getFiveMinute() == null ? 0 : item.getFiveMinute().getLowPriceVolume() * 12;
	}

	private int highSideVolume(MarketItem item)
	{
		if (item.getOneHour() != null)
		{
			return item.getOneHour().getHighPriceVolume();
		}
		return item.getFiveMinute() == null ? 0 : item.getFiveMinute().getHighPriceVolume() * 12;
	}

	private double sideBalance(MarketItem item)
	{
		int lowSide = lowSideVolume(item);
		int highSide = highSideVolume(item);
		int weakerSide = Math.min(lowSide, highSide);
		int strongerSide = Math.max(lowSide, highSide);
		if (strongerSide <= 0)
		{
			return 0;
		}
		return weakerSide / (double) strongerSide;
	}

	private boolean isHistoricallyActionable(HistoricalSignal signal)
	{
		return signal.getMarketState() == HistoricalSignal.MarketState.STABLE
			|| signal.getMarketState() == HistoricalSignal.MarketState.RANGE_BOUND;
	}

	private String marketState(HistoricalSignal signal)
	{
		return signal == null ? "UNKNOWN" : signal.getMarketState().name();
	}

	private String marketWarning(HistoricalSignal signal)
	{
		return signal == null ? "" : signal.getWarning();
	}
}
