package com.merchlens;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GeTax
{
	private static final Set<String> EXEMPT_NAMES = new HashSet<>(Arrays.asList(
		"Old school bond",
		"Chisel",
		"Gardening trowel",
		"Glassblowing pipe",
		"Hammer",
		"Needle",
		"Pestle and mortar",
		"Rake",
		"Saw",
		"Secateurs",
		"Seed dibber",
		"Shears",
		"Spade",
		"Watering can(0)",
		"Watering can (0)"
	));

	private GeTax()
	{
	}

	public static boolean isExempt(String itemName)
	{
		return EXEMPT_NAMES.contains(itemName);
	}

	public static int tax(int sellPrice, String itemName)
	{
		if (sellPrice <= 0 || isExempt(itemName))
		{
			return 0;
		}
		return Math.min((int) (sellPrice * 0.02), 5_000_000);
	}

	public static int netMargin(int buyPrice, int sellPrice, String itemName)
	{
		return sellPrice - buyPrice - tax(sellPrice, itemName);
	}
}
