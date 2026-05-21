package com.merchlens;

import com.merchlens.model.HistoricalSignal;
import com.merchlens.model.TimeseriesPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class HistoricalSignalAnalyzer
{
	HistoricalSignal analyze(int itemId, int currentLow, int currentHigh, List<TimeseriesPoint> points)
	{
		List<Integer> lows = new ArrayList<>();
		List<Integer> mids = new ArrayList<>();
		List<Integer> highs = new ArrayList<>();
		List<Integer> spreads = new ArrayList<>();
		List<TimeseriesPoint> sortedPoints = new ArrayList<>(points);
		sortedPoints.sort(Comparator.comparingLong(TimeseriesPoint::getTimestamp));
		for (TimeseriesPoint point : sortedPoints)
		{
			if (point.hasPrices() && point.getTotalVolume() > 0)
			{
				lows.add(point.getAvgLowPrice());
				mids.add(point.midpoint());
				highs.add(point.getAvgHighPrice());
				spreads.add(point.spread());
			}
		}
		if (mids.size() < 48)
		{
			return new HistoricalSignal(itemId, HistoricalSignal.MarketState.INSUFFICIENT_HISTORY, 0, 0, 0, 1, 0, 1, "Not enough 24h history.");
		}

		int currentMid = (currentLow + currentHigh) / 2;
		int median = median(mids);
		int p10 = percentile(mids, 0.10);
		int p90 = percentile(mids, 0.90);
		int lowP25 = percentile(lows, 0.25);
		int lowP05 = percentile(lows, 0.05);
		int highP75 = percentile(highs, 0.75);
		int medianSpread = Math.max(1, median(spreads));
		double deviation = median <= 0 ? 0 : (currentMid - median) / (double) median;
		double absoluteDeviation = currentMid - median;
		double rangePosition = p90 == p10 ? 0.5 : (currentMid - p10) / (double) (p90 - p10);
		double volatility = median <= 0 ? 1 : (p90 - p10) / (double) median;
		double trend = trend(mids);
		boolean risingBands = risingBands(median, mids, lows, highs);
		double spreadRatio = (currentHigh - currentLow) / (double) medianSpread;
		boolean rangeBound = isRangeBound(mids, lows, highs, currentLow, currentHigh, median, lowP05, lowP25, highP75, volatility, trend);

		HistoricalSignal.MarketState state = state(currentLow, currentMid, median, absoluteDeviation, deviation, rangePosition, volatility, trend, spreadRatio, rangeBound, risingBands);
		return new HistoricalSignal(itemId, state, median, deviation, rangePosition, volatility, trend, spreadRatio, warning(state, deviation, rangePosition, volatility, spreadRatio));
	}

	private HistoricalSignal.MarketState state(
		int currentLow,
		int currentMid,
		int median,
		double absoluteDeviation,
		double deviation,
		double rangePosition,
		double volatility,
		double trend,
		double spreadRatio,
		boolean rangeBound,
		boolean risingBands)
	{
		double cheapSpikeGp = median < 50 ? 3 : Math.max(5, median * 0.08);
		if (risingBands)
		{
			return HistoricalSignal.MarketState.RISING;
		}
		if (rangeBound)
		{
			return HistoricalSignal.MarketState.RANGE_BOUND;
		}
		if ((deviation > 0.10 || absoluteDeviation > cheapSpikeGp) && currentLow > median && rangePosition > 0.80)
		{
			return HistoricalSignal.MarketState.SPIKING;
		}
		if (deviation < -0.10 && trend < -0.03)
		{
			return HistoricalSignal.MarketState.CRASHING;
		}
		if (trend < -0.04)
		{
			return HistoricalSignal.MarketState.FALLING;
		}
		if (trend > 0.04)
		{
			return HistoricalSignal.MarketState.RISING;
		}
		if (Math.abs(deviation) <= 0.08 && rangePosition <= 0.75 && spreadRatio <= 3.0)
		{
			return HistoricalSignal.MarketState.STABLE;
		}
		if (volatility > 0.75 || (volatility > 0.35 && spreadRatio > 4.0))
		{
			return HistoricalSignal.MarketState.UNSTABLE;
		}
		if (currentMid >= median)
		{
			return currentLow > median ? HistoricalSignal.MarketState.SPIKING : HistoricalSignal.MarketState.STABLE;
		}
		return HistoricalSignal.MarketState.FALLING;
	}

	private boolean isRangeBound(
		List<Integer> mids,
		List<Integer> lows,
		List<Integer> highs,
		int currentLow,
		int currentHigh,
		int median,
		int lowP05,
		int lowP25,
		int highP75,
		double volatility,
		double trend)
	{
		if (median <= 0 || volatility < 0.12)
		{
			return false;
		}
		int gpTolerance = Math.max(2, (int) Math.round(median * 0.015));
		if (currentLow > median || currentLow < lowP05 - gpTolerance || currentHigh > highP75 + Math.max(gpTolerance, median / 10))
		{
			return false;
		}

		int lowerTouches = 0;
		int upperTouches = 0;
		for (int low : lows)
		{
			if (low <= lowP25)
			{
				lowerTouches++;
			}
		}
		for (int high : highs)
		{
			if (high >= highP75)
			{
				upperTouches++;
			}
		}

		int minimumTouches = Math.max(8, mids.size() / 10);
		return lowerTouches >= minimumTouches
			&& upperTouches >= minimumTouches
			&& medianCrossings(mids, median) >= 4
			&& (Math.abs(trend) <= 0.18 || repeatedBandsInBothHalves(lows, highs, lowP25, highP75, minimumTouches / 2));
	}

	private boolean risingBands(int median, List<Integer> mids, List<Integer> lows, List<Integer> highs)
	{
		int midShift = splitPercentileShift(mids, 0.50);
		int lowBandShift = splitPercentileShift(lows, 0.25);
		int highBandShift = splitPercentileShift(highs, 0.75);
		int medianThreshold = median < 50 ? 3 : Math.max(50, (int) Math.round(median * 0.010));
		int bandThreshold = median < 50 ? 2 : Math.max(25, (int) Math.round(median * 0.006));
		return midShift >= medianThreshold
			&& lowBandShift >= bandThreshold
			&& highBandShift >= bandThreshold;
	}

	private int splitPercentileShift(List<Integer> values, double percentile)
	{
		if (values.size() < 48)
		{
			return 0;
		}
		int midpoint = values.size() / 2;
		int first = percentile(values.subList(0, midpoint), percentile);
		int second = percentile(values.subList(midpoint, values.size()), percentile);
		return second - first;
	}

	private boolean repeatedBandsInBothHalves(List<Integer> lows, List<Integer> highs, int floor, int ceiling, int minimumTouches)
	{
		int midpoint = lows.size() / 2;
		return countAtOrBelow(lows, 0, midpoint, floor) >= minimumTouches
			&& countAtOrBelow(lows, midpoint, lows.size(), floor) >= minimumTouches
			&& countAtOrAbove(highs, 0, midpoint, ceiling) >= minimumTouches
			&& countAtOrAbove(highs, midpoint, highs.size(), ceiling) >= minimumTouches;
	}

	private String warning(HistoricalSignal.MarketState state, double deviation, double rangePosition, double volatility, double spreadRatio)
	{
		switch (state)
		{
			case STABLE:
				return "Price has been stable versus recent history.";
			case RANGE_BOUND:
				return "Price has repeatedly moved between recent lows and highs; verify the live margin before buying.";
			case RISING:
				return "Price is rising; use smaller margins and verify before buying.";
			case FALLING:
				return "Price is falling versus recent history.";
			case SPIKING:
				return "Current price is elevated versus recent history.";
			case CRASHING:
				return "Current price is below recent median and trending down.";
			case UNSTABLE:
				return "Trend is unclear enough that this needs manual verification.";
			default:
				return "Trend confidence is limited; verify manually.";
		}
	}

	private double trend(List<Integer> mids)
	{
		int size = mids.size();
		List<Integer> recent = mids.subList(Math.max(0, size - 24), size);
		List<Integer> prior = mids.subList(0, Math.max(1, size - 24));
		int recentMedian = median(recent);
		int priorMedian = median(prior);
		return priorMedian <= 0 ? 0 : (recentMedian - priorMedian) / (double) priorMedian;
	}

	private int medianCrossings(List<Integer> mids, int median)
	{
		int crossings = 0;
		int previousSide = 0;
		for (int mid : mids)
		{
			int side = Integer.compare(mid, median);
			if (side == 0)
			{
				continue;
			}
			if (previousSide != 0 && side != previousSide)
			{
				crossings++;
			}
			previousSide = side;
		}
		return crossings;
	}

	private int countAtOrBelow(List<Integer> values, int start, int end, int threshold)
	{
		int count = 0;
		for (int i = start; i < end; i++)
		{
			if (values.get(i) <= threshold)
			{
				count++;
			}
		}
		return count;
	}

	private int countAtOrAbove(List<Integer> values, int start, int end, int threshold)
	{
		int count = 0;
		for (int i = start; i < end; i++)
		{
			if (values.get(i) >= threshold)
			{
				count++;
			}
		}
		return count;
	}

	private int median(List<Integer> values)
	{
		return percentile(values, 0.50);
	}

	private int percentile(List<Integer> values, double percentile)
	{
		List<Integer> sorted = new ArrayList<>(values);
		Collections.sort(sorted);
		int index = (int) Math.round((sorted.size() - 1) * percentile);
		return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
	}
}
