package com.merchlens.model;

public class HistoricalSignal
{
	public enum MarketState
	{
		STABLE,
		RANGE_BOUND,
		RISING,
		FALLING,
		SPIKING,
		CRASHING,
		STALE,
		UNSTABLE,
		INSUFFICIENT_HISTORY
	}

	private final int itemId;
	private final MarketState marketState;
	private final int median24h;
	private final double deviationFromMedian;
	private final double rangePosition;
	private final double volatility;
	private final double trend;
	private final double spreadRatio;
	private final String warning;

	public HistoricalSignal(
		int itemId,
		MarketState marketState,
		int median24h,
		double deviationFromMedian,
		double rangePosition,
		double volatility,
		double trend,
		double spreadRatio,
		String warning)
	{
		this.itemId = itemId;
		this.marketState = marketState;
		this.median24h = median24h;
		this.deviationFromMedian = deviationFromMedian;
		this.rangePosition = rangePosition;
		this.volatility = volatility;
		this.trend = trend;
		this.spreadRatio = spreadRatio;
		this.warning = warning;
	}

	public int getItemId()
	{
		return itemId;
	}

	public MarketState getMarketState()
	{
		return marketState;
	}

	public int getMedian24h()
	{
		return median24h;
	}

	public double getDeviationFromMedian()
	{
		return deviationFromMedian;
	}

	public double getRangePosition()
	{
		return rangePosition;
	}

	public double getVolatility()
	{
		return volatility;
	}

	public double getTrend()
	{
		return trend;
	}

	public double getSpreadRatio()
	{
		return spreadRatio;
	}

	public String getWarning()
	{
		return warning;
	}
}
