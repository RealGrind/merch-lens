package com.merchlens.model;

public class LatestPrice
{
	private Number high;
	private Long highTime;
	private Number low;
	private Long lowTime;

	public Integer getHigh()
	{
		return PriceValue.asInt(high);
	}

	public Long getHighTime()
	{
		return highTime;
	}

	public Integer getLow()
	{
		return PriceValue.asInt(low);
	}

	public Long getLowTime()
	{
		return lowTime;
	}

	public boolean hasUnsupportedPrice()
	{
		return (high != null && getHigh() == null) || (low != null && getLow() == null);
	}
}
