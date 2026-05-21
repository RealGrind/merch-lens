package com.merchlens;

import com.google.gson.Gson;
import com.merchlens.model.FlipRecord;
import java.util.List;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Assert;
import org.junit.Test;

public class FlipHistoryStoreTest
{
	@Test
	public void splitSellConsumesOnlyMatchingBuyQuantity()
	{
		FlipHistoryStore store = new FlipHistoryStore(null, new Gson());
		store.record(snapshot(GrandExchangeOfferState.BOUGHT, 385, 95, 100, 100, 9_500, 1));
		store.record(snapshot(GrandExchangeOfferState.SOLD, 385, 100, 70, 70, 7_000, 2));

		List<FlipRecord> open = store.openFlips();
		List<FlipRecord> closed = store.recentClosed();

		Assert.assertEquals(1, open.size());
		Assert.assertEquals(30, open.get(0).getBuyQuantity());
		Assert.assertEquals(2_850, open.get(0).getBuySpent());
		Assert.assertEquals(1, closed.size());
		Assert.assertEquals(70, closed.get(0).getSellQuantity());
		Assert.assertEquals(6_650, closed.get(0).getBuySpent());
		Assert.assertEquals(7_000, closed.get(0).getSellReceived());
		Assert.assertEquals(350, closed.get(0).getGrossProfit());
	}

	@Test
	public void secondSellClosesRemainingQuantityAtDifferentPrice()
	{
		FlipHistoryStore store = new FlipHistoryStore(null, new Gson());
		store.record(snapshot(GrandExchangeOfferState.BOUGHT, 385, 95, 100, 100, 9_500, 1));
		store.record(snapshot(GrandExchangeOfferState.SOLD, 385, 100, 70, 70, 7_000, 2));
		store.record(snapshot(GrandExchangeOfferState.SOLD, 385, 90, 30, 30, 2_700, 3));

		List<FlipRecord> open = store.openFlips();
		List<FlipRecord> closed = store.recentClosed();

		Assert.assertTrue(open.isEmpty());
		Assert.assertEquals(2, closed.size());
		Assert.assertEquals(100, store.summary().getTotalQuantity());
		Assert.assertEquals(200, store.summary().getGrossProfit());
	}

	private OfferSnapshot snapshot(GrandExchangeOfferState state, int itemId, int price, int totalQuantity, int filledQuantity, int spent, long updatedAt)
	{
		return new OfferSnapshot(new StubOffer(state, itemId, price, totalQuantity, filledQuantity, spent), updatedAt);
	}

	private static class StubOffer implements GrandExchangeOffer
	{
		private final GrandExchangeOfferState state;
		private final int itemId;
		private final int price;
		private final int totalQuantity;
		private final int filledQuantity;
		private final int spent;

		StubOffer(GrandExchangeOfferState state, int itemId, int price, int totalQuantity, int filledQuantity, int spent)
		{
			this.state = state;
			this.itemId = itemId;
			this.price = price;
			this.totalQuantity = totalQuantity;
			this.filledQuantity = filledQuantity;
			this.spent = spent;
		}

		@Override
		public int getQuantitySold()
		{
			return filledQuantity;
		}

		@Override
		public int getItemId()
		{
			return itemId;
		}

		@Override
		public int getTotalQuantity()
		{
			return totalQuantity;
		}

		@Override
		public int getPrice()
		{
			return price;
		}

		@Override
		public int getSpent()
		{
			return spent;
		}

		@Override
		public GrandExchangeOfferState getState()
		{
			return state;
		}
	}
}
