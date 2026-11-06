package com.merchlens.model;

public class TimeseriesPoint
{
	private long timestamp;
	private Number avgHighPrice;
	private Number avgLowPrice;
	private int highPriceVolume;
	private int lowPriceVolume;

	public long getTimestamp()
	{
		return timestamp;
	}

	public Integer getAvgHighPrice()
	{
		return PriceValue.asInt(avgHighPrice);
	}

	public Integer getAvgLowPrice()
	{
		return PriceValue.asInt(avgLowPrice);
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
		Integer high = getAvgHighPrice();
		Integer low = getAvgLowPrice();
		return high != null && low != null && high > 0 && low > 0;
	}

	public int midpoint()
	{
		return (int) (((long) getAvgHighPrice() + getAvgLowPrice()) / 2);
	}

	public int spread()
	{
		return Math.max(0, getAvgHighPrice() - getAvgLowPrice());
	}
}
