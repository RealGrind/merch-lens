package com.merchlens.model;

public class TimeseriesPoint
{
	private long timestamp;
	private Integer avgHighPrice;
	private Integer avgLowPrice;
	private int highPriceVolume;
	private int lowPriceVolume;

	public long getTimestamp()
	{
		return timestamp;
	}

	public Integer getAvgHighPrice()
	{
		return avgHighPrice;
	}

	public Integer getAvgLowPrice()
	{
		return avgLowPrice;
	}

	public int getHighPriceVolume()
	{
		return highPriceVolume;
	}

	public int getLowPriceVolume()
	{
		return lowPriceVolume;
	}

	public int getTotalVolume()
	{
		return Math.max(0, highPriceVolume) + Math.max(0, lowPriceVolume);
	}

	public boolean hasPrices()
	{
		return avgHighPrice != null && avgLowPrice != null && avgHighPrice > 0 && avgLowPrice > 0;
	}

	public int midpoint()
	{
		return (avgHighPrice + avgLowPrice) / 2;
	}

	public int spread()
	{
		return Math.max(0, avgHighPrice - avgLowPrice);
	}
}
