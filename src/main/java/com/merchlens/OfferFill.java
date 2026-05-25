package com.merchlens;

final class OfferFill
{
	enum Side
	{
		BUY,
		SELL
	}

	private final Side side;
	private final int itemId;
	private final int quantity;
	private final long totalValue;
	private final long filledAt;
	private final String offerKey;

	OfferFill(Side side, int itemId, int quantity, long totalValue, long filledAt)
	{
		this(side, itemId, quantity, totalValue, filledAt, side + ":" + itemId + ":" + filledAt);
	}

	OfferFill(Side side, int itemId, int quantity, long totalValue, long filledAt, String offerKey)
	{
		this.side = side;
		this.itemId = itemId;
		this.quantity = quantity;
		this.totalValue = totalValue;
		this.filledAt = filledAt;
		this.offerKey = offerKey;
	}

	Side getSide()
	{
		return side;
	}

	int getItemId()
	{
		return itemId;
	}

	int getQuantity()
	{
		return quantity;
	}

	long getTotalValue()
	{
		return totalValue;
	}

	long getFilledAt()
	{
		return filledAt;
	}

	String getOfferKey()
	{
		return offerKey;
	}
}
