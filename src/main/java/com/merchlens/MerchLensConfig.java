package com.merchlens;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("merchlens")
public interface MerchLensConfig extends Config
{
	@ConfigItem(
		keyName = "budget",
		name = "Bank size",
		description = "Maximum GP price per item to show in recommendations.",
		hidden = true
	)
	default int budget()
	{
		return 50_000_000;
	}

	@ConfigItem(
		keyName = "minimumProfit",
		name = "Minimum profit",
		description = "Minimum expected profit per recommendation.",
		hidden = true
	)
	default int minimumProfit()
	{
		return 25_000;
	}

	@ConfigItem(
		keyName = "screenerMinPrice",
		name = "Screener min price",
		description = "Hidden UI-backed Screener minimum item price.",
		hidden = true
	)
	default int screenerMinPrice()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "screenerMaxPrice",
		name = "Screener max price",
		description = "Hidden UI-backed Screener maximum item price. Zero means no maximum.",
		hidden = true
	)
	default int screenerMaxPrice()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "screenerMinBuyVolume",
		name = "Screener min buy volume",
		description = "Hidden UI-backed Screener minimum buy-side volume per hour.",
		hidden = true
	)
	default int screenerMinBuyVolume()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "screenerMinSellVolume",
		name = "Screener min sell volume",
		description = "Hidden UI-backed Screener minimum sell-side volume per hour.",
		hidden = true
	)
	default int screenerMinSellVolume()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "screenerBuySellRatio",
		name = "Screener buy/sell ratio",
		description = "Hidden UI-backed Screener minimum buy/sell volume balance.",
		hidden = true
	)
	default double screenerBuySellRatio()
	{
		return 0;
	}
}
