package com.merchlens;

import org.junit.Assert;
import org.junit.Test;

public class GeTaxTest
{
	@Test
	public void taxIsTwoPercentRoundedDown()
	{
		Assert.assertEquals(2, GeTax.tax(101, "Abyssal whip"));
		Assert.assertEquals(0, GeTax.tax(49, "Abyssal whip"));
		Assert.assertEquals(1, GeTax.tax(50, "Abyssal whip"));
	}

	@Test
	public void taxIsCappedAtFiveMillion()
	{
		Assert.assertEquals(5_000_000, GeTax.tax(250_000_000, "Twisted bow"));
		Assert.assertEquals(5_000_000, GeTax.tax(1_000_000_000, "Twisted bow"));
	}

	@Test
	public void exemptItemsHaveNoTax()
	{
		Assert.assertEquals(0, GeTax.tax(1_000_000, "Hammer"));
		Assert.assertEquals(100_000, GeTax.netMargin(900_000, 1_000_000, "Hammer"));
	}
}
