package com.merchlens.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class PriceValue
{
	private PriceValue()
	{
	}

	static Integer asInt(Number value)
	{
		if (value == null)
		{
			return null;
		}
		try
		{
			BigDecimal price = new BigDecimal(value.toString());
			if (price.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0
				|| price.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0)
			{
				return null;
			}
			return price.setScale(0, RoundingMode.HALF_UP).intValueExact();
		}
		catch (NumberFormatException | ArithmeticException ex)
		{
			return null;
		}
	}
}
