package com.merchlens;

import com.google.gson.Gson;
import java.util.List;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Assert;
import org.junit.Test;

public class OfferTrackerTest
{
	@Test
	public void startupOfferWithoutSavedStateShowsUnknownTime()
	{
		OfferTracker tracker = new OfferTracker(null, new Gson());

		tracker.record(3, offer(GrandExchangeOfferState.BUYING, 4151, 1_000_000, 8, 2, 2_000_000));

		List<TrackedOffer> offers = tracker.activeOffers();
		Assert.assertEquals(1, offers.size());
		Assert.assertEquals(3, offers.get(0).getSlot());
		Assert.assertFalse(offers.get(0).isTrackedStartKnown());
		Assert.assertEquals(2, offers.get(0).getFilledQuantity());
		Assert.assertEquals(8, offers.get(0).getTotalQuantity());
	}

	@Test
	public void matchingSlotUpdatePreservesOriginalTrackedStart()
	{
		OfferTracker tracker = new OfferTracker(null, new Gson());

		tracker.record(1, offer(GrandExchangeOfferState.SELLING, 385, 100, 100, 10, 1_000));
		TrackedOffer first = tracker.activeOffers().get(0);
		long firstSeenAt = first.getFirstSeenAt();

		tracker.record(1, offer(GrandExchangeOfferState.SELLING, 385, 100, 100, 70, 7_000));
		TrackedOffer updated = tracker.activeOffers().get(0);

		Assert.assertEquals(firstSeenAt, updated.getFirstSeenAt());
		Assert.assertEquals(70, updated.getFilledQuantity());
		Assert.assertEquals(7_000, updated.getSpent());
	}

	@Test
	public void matchingOfferAllowsProgressButRejectsRegressions()
	{
		TrackedOffer tracked = new TrackedOffer(
			2,
			offer(GrandExchangeOfferState.BUYING, 11840, 1_000_000, 10, 3, 3_000_000),
			100,
			true
		);

		Assert.assertTrue(tracked.matches(offer(GrandExchangeOfferState.BUYING, 11840, 1_000_000, 10, 7, 7_000_000)));
		Assert.assertFalse(tracked.matches(offer(GrandExchangeOfferState.BUYING, 11840, 1_000_000, 10, 0, 0)));
		Assert.assertFalse(tracked.matches(offer(GrandExchangeOfferState.BUYING, 11840, 1_000_000, 10, 7, 2_000_000)));
	}

	@Test
	public void trackedPendingBuyEmitsActualFillValueWhenObservedCompleted()
	{
		OfferTracker tracker = new OfferTracker(null, new Gson());
		tracker.record(1, offer(GrandExchangeOfferState.BUYING, 560, 100, 5_000, 0, 0));

		OfferFill fill = tracker.record(1, offer(GrandExchangeOfferState.BOUGHT, 560, 100, 5_000, 1_200, 117_600));

		Assert.assertNotNull(fill);
		Assert.assertEquals(OfferFill.Side.BUY, fill.getSide());
		Assert.assertEquals(1_200, fill.getQuantity());
		Assert.assertEquals(117_600, fill.getTotalValue());
	}

	private GrandExchangeOffer offer(GrandExchangeOfferState state, int itemId, int price, int totalQuantity, int filledQuantity, int spent)
	{
		return new StubOffer(state, itemId, price, totalQuantity, filledQuantity, spent);
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
