package com.merchlens.model;

public class PriceWindow
{
	private Integer avgHighPrice;
	private int highPriceVolume;
	private Integer avgLowPrice;
	private int lowPriceVolume;

	public Integer getAvgHighPrice()
	{
		return avgHighPrice;
	}

	public int getHighPriceVolume()
	{
		return highPriceVolume;
	}

	public Integer getAvgLowPrice()
	{
		return avgLowPrice;
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
