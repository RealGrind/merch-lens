package com.merchlens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merchlens.model.LatestPrice;
import com.merchlens.model.PriceWindow;
import com.merchlens.model.TimeseriesPoint;
import com.merchlens.model.TimeseriesResponse;
import com.merchlens.model.WikiEnvelope;
import java.lang.reflect.Type;
import org.junit.Assert;
import org.junit.Test;

public class WikiPriceParsingTest
{
	private final Gson gson = new Gson();

	@Test
	public void expensiveItemDoesNotPreventNormalPricesFromLoading()
	{
		Type type = new TypeToken<WikiEnvelope<LatestPrice>>() {}.getType();
		WikiEnvelope<LatestPrice> prices = gson.fromJson(
			"{\"data\":{\"1\":{\"high\":8800000000,\"low\":8669336500},"
				+ "\"2\":{\"high\":1300,\"low\":1000}}}", type);

		Assert.assertNull(prices.getData().get(1).getHigh());
		Assert.assertNull(prices.getData().get(1).getLow());
		Assert.assertTrue(prices.getData().get(1).hasUnsupportedPrice());
		Assert.assertEquals(Integer.valueOf(1300), prices.getData().get(2).getHigh());
		Assert.assertEquals(Integer.valueOf(1000), prices.getData().get(2).getLow());
		Assert.assertFalse(prices.getData().get(2).hasUnsupportedPrice());
	}

	@Test
	public void fractionalAveragesRoundWithoutOverflow()
	{
		Type type = new TypeToken<WikiEnvelope<PriceWindow>>() {}.getType();
		WikiEnvelope<PriceWindow> prices = gson.fromJson(
			"{\"data\":{\"1\":{\"avgHighPrice\":8800000000.25,\"avgLowPrice\":8669336500.50},"
				+ "\"2\":{\"avgHighPrice\":1300.25,\"avgLowPrice\":999.50},"
				+ "\"3\":{\"avgHighPrice\":2147483647.25}}}", type);

		Assert.assertNull(prices.getData().get(1).getAvgHighPrice());
		Assert.assertNull(prices.getData().get(1).getAvgLowPrice());
		Assert.assertEquals(Integer.valueOf(1300), prices.getData().get(2).getAvgHighPrice());
		Assert.assertEquals(Integer.valueOf(1000), prices.getData().get(2).getAvgLowPrice());
		Assert.assertNull(prices.getData().get(3).getAvgHighPrice());
	}

	@Test
	public void timeseriesSkipsUnsupportedPointWithoutLosingOtherPoints()
	{
		TimeseriesResponse response = gson.fromJson(
			"{\"data\":[{\"timestamp\":1,\"avgHighPrice\":8800000000.25,\"avgLowPrice\":8669336500.50},"
				+ "{\"timestamp\":2,\"avgHighPrice\":1300.25,\"avgLowPrice\":999.50}]}",
			TimeseriesResponse.class);
		TimeseriesPoint unsupported = response.getData().get(0);
		TimeseriesPoint supported = response.getData().get(1);

		Assert.assertFalse(unsupported.hasPrices());
		Assert.assertTrue(supported.hasPrices());
		Assert.assertEquals(1150, supported.midpoint());
	}
}
