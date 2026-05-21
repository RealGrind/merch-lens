package com.merchlens.model;

public class OfferAdvice
{
	private final int itemId;
	private final String recordId;
	private final String itemName;
	private final String state;
	private final String action;
	private final int offerPrice;
	private final int filledQuantity;
	private final int averageBuyPrice;
	private final int targetSellPrice;
	private final int netMargin;
	private final String note;

	public OfferAdvice(
		int itemId,
		String recordId,
		String itemName,
		String state,
		String action,
		int offerPrice,
		int filledQuantity,
		int averageBuyPrice,
		int targetSellPrice,
		int netMargin,
		String note)
	{
		this.itemId = itemId;
		this.recordId = recordId;
		this.itemName = itemName;
		this.state = state;
		this.action = action;
		this.offerPrice = offerPrice;
		this.filledQuantity = filledQuantity;
		this.averageBuyPrice = averageBuyPrice;
		this.targetSellPrice = targetSellPrice;
		this.netMargin = netMargin;
		this.note = note;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getRecordId()
	{
		return recordId;
	}

	public String getItemName()
	{
		return itemName;
	}

	public String getState()
	{
		return state;
	}

	public String getAction()
	{
		return action;
	}

	public int getOfferPrice()
	{
		return offerPrice;
	}

	public int getFilledQuantity()
	{
		return filledQuantity;
	}

	public int getAverageBuyPrice()
	{
		return averageBuyPrice;
	}

	public int getTargetSellPrice()
	{
		return targetSellPrice;
	}

	public int getNetMargin()
	{
		return netMargin;
	}

	public String getNote()
	{
		return note;
	}
}
