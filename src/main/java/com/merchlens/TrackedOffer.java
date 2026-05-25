package com.merchlens;

import net.runelite.api.GrandExchangeOffer;

class TrackedOffer
{
	private int slot;
	private int itemId;
	private int price;
	private int totalQuantity;
	private int filledQuantity;
	private int spent;
	private String state;
	private long firstSeenAt;
	private long lastUpdatedAt;
	private boolean trackedStartKnown;

	TrackedOffer()
	{
	}

	TrackedOffer(int slot, GrandExchangeOffer offer, long now, boolean trackedStartKnown)
	{
		this.slot = slot;
		this.firstSeenAt = now;
		this.trackedStartKnown = trackedStartKnown;
		update(offer, now);
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

	int getTotalQuantity()
	{
		return totalQuantity;
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

	long getFirstSeenAt()
	{
		return firstSeenAt;
	}

	long getLastUpdatedAt()
	{
		return lastUpdatedAt;
	}

	boolean isTrackedStartKnown()
	{
		return trackedStartKnown;
	}

	boolean isBuyOffer()
	{
		return isBuyState(state);
	}

	String offerKey()
	{
		return slot + ":" + firstSeenAt + ":" + itemId + ":" + price + ":" + totalQuantity + ":" + (isBuyOffer() ? "BUY" : "SELL");
	}

	boolean isVisible()
	{
		return itemId > 0 && totalQuantity > 0 && state != null && !"EMPTY".equals(state);
	}

	boolean matches(GrandExchangeOffer offer)
	{
		if (offer == null || offer.getState() == null)
		{
			return false;
		}
		return itemId == offer.getItemId()
			&& price == offer.getPrice()
			&& totalQuantity == offer.getTotalQuantity()
			&& offer.getQuantitySold() >= filledQuantity
			&& offer.getSpent() >= spent
			&& sameSide(state, offer.getState().name());
	}

	void update(GrandExchangeOffer offer, long now)
	{
		itemId = offer.getItemId();
		price = offer.getPrice();
		totalQuantity = offer.getTotalQuantity();
		filledQuantity = offer.getQuantitySold();
		spent = offer.getSpent();
		state = offer.getState().name();
		lastUpdatedAt = now;
	}

	private boolean sameSide(String left, String right)
	{
		if (left == null || right == null)
		{
			return false;
		}
		return isBuyState(left) == isBuyState(right) && isSellState(left) == isSellState(right);
	}

	private boolean isBuyState(String value)
	{
		return value.contains("BUY") || "BOUGHT".equals(value);
	}

	private boolean isSellState(String value)
	{
		return value.contains("SELL") || "SOLD".equals(value);
	}
}
