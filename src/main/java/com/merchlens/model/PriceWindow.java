package com.merchlens.model;

public class PriceWindow
{
	private Number avgHighPrice;
	private int highPriceVolume;
	private Number avgLowPrice;
	private int lowPriceVolume;

	public Integer getAvgHighPrice()
	{
		return PriceValue.asInt(avgHighPrice);
	}

	public int getHighPriceVolume()
	{
		return highPriceVolume;
	}

	public Integer getAvgLowPrice()
	{
		return PriceValue.asInt(avgLowPrice);
	}

	public int getLowPriceVolume()
	{
		return lowPriceVolume;
	}

	public int getTotalVolume()
	{
		return Math.max(0, highPriceVolume) + Math.max(0, lowPriceVolume);
	}
}
