package com.merchlens.model;

public class MarketItem
{
	private final ItemMetadata metadata;
	private final LatestPrice latestPrice;
	private final PriceWindow fiveMinute;
	private final PriceWindow oneHour;

	public MarketItem(ItemMetadata metadata, LatestPrice latestPrice, PriceWindow fiveMinute, PriceWindow oneHour)
	{
		this.metadata = metadata;
		this.latestPrice = latestPrice;
		this.fiveMinute = fiveMinute;
		this.oneHour = oneHour;
	}

	public ItemMetadata getMetadata()
	{
		return metadata;
	}

	public LatestPrice getLatestPrice()
	{
		return latestPrice;
	}

	public PriceWindow getFiveMinute()
	{
		return fiveMinute;
	}

	public PriceWindow getOneHour()
	{
		return oneHour;
	}
}
