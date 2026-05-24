package com.merchlens.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RecommendationDto
{
	private int itemId;
	private String itemName;
	private String action;
	private String strategy;
	private String risk;
	private int buyPrice;
	private int sellPrice;
	private int quantity;
	private int buyLimit;
	private int tax;
	private int netMargin;
	private int expectedProfit;
	private double roi;
	private int capitalRequired;
	private double expectedHoldHours;
	private double slotEfficiency;
	private double capitalEfficiency;
	private double score;
	private int hourlyVolume;
	private int buyVolumePerHour;
	private int sellVolumePerHour;
	private int latestAgeMinutes;
	private String marketState;
	private String marketWarning;
	private String explanation;
	private List<String> warnings;
	private Map<String, Double> confidence;

	public RecommendationDto(
		int itemId,
		String itemName,
		String action,
		String strategy,
		String risk,
		int buyPrice,
		int sellPrice,
		int quantity,
		int buyLimit,
		int tax,
		int netMargin,
		int expectedProfit,
		double roi,
		int capitalRequired,
		double expectedHoldHours,
		double slotEfficiency,
		double capitalEfficiency,
		double score,
		int hourlyVolume,
		int buyVolumePerHour,
		int sellVolumePerHour,
		int latestAgeMinutes,
		String marketState,
		String marketWarning,
		String explanation,
		List<String> warnings,
		Map<String, Double> confidence)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.action = action;
		this.strategy = strategy;
		this.risk = risk;
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.quantity = quantity;
		this.buyLimit = buyLimit;
		this.tax = tax;
		this.netMargin = netMargin;
		this.expectedProfit = expectedProfit;
		this.roi = roi;
		this.capitalRequired = capitalRequired;
		this.expectedHoldHours = expectedHoldHours;
		this.slotEfficiency = slotEfficiency;
		this.capitalEfficiency = capitalEfficiency;
		this.score = score;
		this.hourlyVolume = hourlyVolume;
		this.buyVolumePerHour = buyVolumePerHour;
		this.sellVolumePerHour = sellVolumePerHour;
		this.latestAgeMinutes = latestAgeMinutes;
		this.marketState = marketState;
		this.marketWarning = marketWarning;
		this.explanation = explanation;
		this.warnings = warnings;
		this.confidence = confidence;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getItemName()
	{
		return itemName;
	}

	public String getAction()
	{
		return action;
	}

	public String getStrategy()
	{
		return strategy;
	}

	public String getRisk()
	{
		return risk;
	}

	public int getBuyPrice()
	{
		return buyPrice;
	}

	public int getSellPrice()
	{
		return sellPrice;
	}

	public int getQuantity()
	{
		return quantity;
	}

	public int getBuyLimit()
	{
		return buyLimit;
	}

	public int getTax()
	{
		return tax;
	}

	public int getNetMargin()
	{
		return netMargin;
	}

	public int getExpectedProfit()
	{
		return expectedProfit;
	}

	public double getRoi()
	{
		return roi;
	}

	public int getCapitalRequired()
	{
		return capitalRequired;
	}

	public double getExpectedHoldHours()
	{
		return expectedHoldHours;
	}

	public double getSlotEfficiency()
	{
		return slotEfficiency;
	}

	public double getCapitalEfficiency()
	{
		return capitalEfficiency;
	}

	public double getScore()
	{
		return score;
	}

	public int getHourlyVolume()
	{
		return hourlyVolume;
	}

	public int getBuyVolumePerHour()
	{
		return buyVolumePerHour;
	}

	public int getSellVolumePerHour()
	{
		return sellVolumePerHour;
	}

	public int getFourHourFlowQuantity()
	{
		int weakerHourlySide = Math.max(0, Math.min(buyVolumePerHour, sellVolumePerHour));
		long observedFourHourFlow = (long) weakerHourlySide * 4L;
		return (int) Math.min(Math.max(0, buyLimit), Math.min(Integer.MAX_VALUE, observedFourHourFlow));
	}

	public long getFourHourFlowProfit()
	{
		return (long) netMargin * getFourHourFlowQuantity();
	}

	public boolean isSeverelyFlowLimited()
	{
		return buyLimit > 0 && (long) getFourHourFlowQuantity() * 4L < buyLimit;
	}

	public int getLatestAgeMinutes()
	{
		return latestAgeMinutes;
	}

	public String getMarketState()
	{
		return marketState;
	}

	public String getMarketWarning()
	{
		return marketWarning;
	}

	public String getExplanation()
	{
		return explanation;
	}

	public List<String> getWarnings()
	{
		return warnings == null ? Collections.emptyList() : warnings;
	}

	public Map<String, Double> getConfidence()
	{
		return confidence == null ? Collections.emptyMap() : confidence;
	}
}
