package com.merchlens;

import net.runelite.api.GrandExchangeOffer;

class OfferSnapshot
{
	private final int slot;
	private final int itemId;
	private final int price;
	private final int quantity;
	private final int filledQuantity;
	private final int spent;
	private final String state;
	private final long updatedAt;

	OfferSnapshot(GrandExchangeOffer offer, long updatedAt)
	{
		this(-1, offer, updatedAt);
	}

	OfferSnapshot(int slot, GrandExchangeOffer offer, long updatedAt)
	{
		this.slot = slot;
		this.itemId = offer.getItemId();
		this.price = offer.getPrice();
		this.quantity = offer.getTotalQuantity();
		this.filledQuantity = offer.getQuantitySold();
		this.spent = offer.getSpent();
		this.state = offer.getState().name();
		this.updatedAt = updatedAt;
	}

	int getSlot()
	{
		return slot;
	}

	int getItemId()
	{
		return itemId;
	}

	int getPrice()
	{
		return price;
	}

	int getQuantity()
	{
		return quantity;
	}

	int getFilledQuantity()
	{
		return filledQuantity;
	}

	int getSpent()
	{
		return spent;
	}

	String getState()
	{
		return state;
	}

	long getUpdatedAt()
	{
		return updatedAt;
	}
}
