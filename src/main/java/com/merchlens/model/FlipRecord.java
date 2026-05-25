package com.merchlens.model;

public class FlipRecord
{
	private int itemId;
	private String itemName;
	private long quantity;
	private long cost;
	private long grossProceeds;
	private long tax;
	private long completedAt;
	private String saleOfferKey;

	public FlipRecord()
	{
	}

	public FlipRecord(int itemId, String itemName, long quantity, long cost, long grossProceeds, long tax, long completedAt, String saleOfferKey)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.quantity = quantity;
		this.cost = cost;
		this.grossProceeds = grossProceeds;
		this.tax = tax;
		this.completedAt = completedAt;
		this.saleOfferKey = saleOfferKey;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getItemName()
	{
		return itemName;
	}

	public long getQuantity()
	{
		return quantity;
	}

	public long getCost()
	{
		return cost;
	}

	public long getGrossProceeds()
	{
		return grossProceeds;
	}

	public long getTax()
	{
		return tax;
	}

	public long getProfit()
	{
		return grossProceeds - tax - cost;
	}

	public double getRoi()
	{
		return cost <= 0 ? 0 : getProfit() / (double) cost;
	}

	public long getCompletedAt()
	{
		return completedAt;
	}

	public String getSaleOfferKey()
	{
		return saleOfferKey;
	}
}
