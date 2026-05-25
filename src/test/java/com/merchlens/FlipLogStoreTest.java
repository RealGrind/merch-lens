package com.merchlens;

import com.google.gson.Gson;
import com.merchlens.model.FlipRecord;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class FlipLogStoreTest
{
	@Test
	public void partialSalesUsePersistedActualBuyCostAndActualSellValue()
	{
		FlipLogStore store = new FlipLogStore(null, new Gson());
		store.record(new OfferFill(OfferFill.Side.BUY, 1, 100, 8_000, 100), "Death rune");
		store.record(new OfferFill(OfferFill.Side.SELL, 1, 70, 7_000, 200), "Death rune");
		store.record(new OfferFill(OfferFill.Side.SELL, 1, 30, 2_700, 300), "Death rune");

		List<FlipRecord> records = store.records();
		Assert.assertEquals(2, records.size());
		Assert.assertEquals(5_600, records.get(0).getCost());
		Assert.assertEquals(1_260, records.get(0).getProfit());
		Assert.assertEquals(2_400, records.get(1).getCost());
		Assert.assertEquals(270, records.get(1).getProfit());
	}

	@Test
	public void salesMatchMultiplePurchaseLotsByExactFifoCost()
	{
		FlipLogStore store = new FlipLogStore(null, new Gson());
		store.record(new OfferFill(OfferFill.Side.BUY, 1, 70, 7_000, 100), "Death rune");
		store.record(new OfferFill(OfferFill.Side.BUY, 1, 30, 2_700, 110), "Death rune");
		store.record(new OfferFill(OfferFill.Side.SELL, 1, 100, 12_000, 200), "Death rune");

		FlipRecord record = store.records().get(0);
		Assert.assertEquals(100, record.getQuantity());
		Assert.assertEquals(9_700, record.getCost());
		Assert.assertEquals(2_100, record.getProfit());
	}

	@Test
	public void saleWithoutRecordedBuyDoesNotInventProfit()
	{
		FlipLogStore store = new FlipLogStore(null, new Gson());
		store.record(new OfferFill(OfferFill.Side.SELL, 1, 100, 12_000, 200), "Death rune");

		Assert.assertTrue(store.records().isEmpty());
	}
}
