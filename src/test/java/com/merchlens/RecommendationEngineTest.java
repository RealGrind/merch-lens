package com.merchlens;

import com.merchlens.model.ItemMetadata;
import com.merchlens.model.LatestPrice;
import com.merchlens.model.MarketItem;
import com.merchlens.model.PriceWindow;
import com.merchlens.model.RecommendationDto;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class RecommendationEngineTest
{
	private final RecommendationEngine engine = new RecommendationEngine();

	@Test
	public void taxNegativeItemIsAvoided() throws Exception
	{
		RecommendationDto recommendation = engine.inspect(item("Coal", 1020, 1000, 10_000, 100_000), config(), null);

		Assert.assertEquals("Avoid", recommendation.getAction());
		Assert.assertTrue(recommendation.getNetMargin() <= 0);
	}

	@Test
	public void highVolumeProfitableItemGetsBuyRecommendation() throws Exception
	{
		RecommendationDto recommendation = engine.highVolumeStaples(
			Collections.singletonList(item("Coal", 1300, 1000, 10_000, 100_000)),
			config()
		).get(0);

		Assert.assertEquals("Buy", recommendation.getAction());
		Assert.assertTrue(recommendation.getExpectedProfit() > 0);
		Assert.assertTrue(recommendation.getConfidence().get("liquidity") > 0.5);
	}

	@Test
	public void veryLowVolumeItemIsNotRecommended() throws Exception
	{
		List<RecommendationDto> recommendations = engine.highVolumeStaples(
			Collections.singletonList(item("Coal", 10_000_000, 5_000_000, 10_000, 1)),
			config()
		);

		Assert.assertTrue(recommendations.isEmpty());
	}

	@Test
	public void oneSidedVolumeItemIsNotRecommended() throws Exception
	{
		List<RecommendationDto> recommendations = engine.highVolumeStaples(
			Collections.singletonList(item("Coal", 10_000_000, 5_000_000, 10_000, 2_000, 2_000, 0)),
			config()
		);

		Assert.assertTrue(recommendations.isEmpty());
	}

	@Test
	public void screenerKeepsLowVolumeItemsForUiFilters() throws Exception
	{
		List<RecommendationDto> recommendations = engine.screener(
			Collections.singletonList(item(1300, 1000, 10_000, 1)),
			config()
		);

		Assert.assertEquals(1, recommendations.size());
		Assert.assertTrue(recommendations.get(0).getNetMargin() > 0);
	}

	@Test
	public void screenerDoesNotApplyMinimumProfitFloor() throws Exception
	{
		RecommendationDto recommendation = engine.screener(
			Collections.singletonList(item(1030, 1000, 1, 1)),
			config()
		).get(0);

		Assert.assertTrue(recommendation.getNetMargin() > 0);
		Assert.assertTrue(recommendation.getExpectedProfit() < config().minimumProfit());
	}

	@Test
	public void highVolumeStaplesUsesExpandedResearchCatalog() throws Exception
	{
		List<RecommendationDto> recommendations = engine.highVolumeStaples(
			Arrays.asList(
				item("Coal", 170, 150, 13_000, 8_000),
				item("Games necklace(8)", 1_000, 950, 10_000, 8_000)
			),
			config()
		);

		List<String> itemNames = recommendations.stream()
			.map(RecommendationDto::getItemName)
			.collect(Collectors.toList());
		Assert.assertTrue(itemNames.contains("Coal"));
		Assert.assertTrue(itemNames.contains("Games necklace(8)"));
	}

	@Test
	public void highVolumeStaplesExcludesGenericGearNotInCatalog() throws Exception
	{
		List<RecommendationDto> recommendations = engine.highVolumeStaples(
			Collections.singletonList(item("Abyssal whip", 1_600_000, 1_500_000, 70, 8_000)),
			config()
		);

		Assert.assertTrue(recommendations.isEmpty());
	}

	@Test
	public void itemAboveBankSizeIsNotRecommended() throws Exception
	{
		List<RecommendationDto> recommendations = engine.highVolumeStaples(
			Collections.singletonList(item("Coal", 75_000_000, 60_000_000, 8, 100_000)),
			config(50_000_000)
		);

		Assert.assertTrue(recommendations.isEmpty());
	}

	@Test
	public void highVolumeStaplesKeepFullBuyLimitForDisplay() throws Exception
	{
		RecommendationDto recommendation = engine.highVolumeStaples(
			Collections.singletonList(item("Coal", 1300, 1000, 100_000, 300_000)),
			config(50_000_000)
		).get(0);

		Assert.assertEquals(100_000, recommendation.getBuyLimit());
		Assert.assertEquals(GeTax.netMargin(1000, 1300, "Coal") * 100_000, recommendation.getExpectedProfit());
	}

	private MerchLensConfig config()
	{
		return config(50_000_000);
	}

	private MerchLensConfig config(int budget)
	{
		return new MerchLensConfig()
		{
			@Override
			public int budget()
			{
				return budget;
			}

			@Override
			public int minimumProfit()
			{
				return 10_000;
			}
		};
	}

	private MarketItem item(int high, int low, int limit, int hourlyVolume) throws Exception
	{
		return item("Test item", high, low, limit, hourlyVolume, hourlyVolume / 2, hourlyVolume / 2);
	}

	private MarketItem item(String name, int high, int low, int limit, int hourlyVolume) throws Exception
	{
		return item(name, high, low, limit, hourlyVolume, hourlyVolume / 2, hourlyVolume / 2);
	}

	private MarketItem item(String name, int high, int low, int limit, int hourlyVolume, int highVolume, int lowVolume) throws Exception
	{
		ItemMetadata metadata = new ItemMetadata();
		set(metadata, "id", Math.abs(name.hashCode()));
		set(metadata, "name", name);
		set(metadata, "members", true);
		set(metadata, "limit", limit);

		LatestPrice latest = new LatestPrice();
		long now = System.currentTimeMillis() / 1000L;
		set(latest, "high", high);
		set(latest, "low", low);
		set(latest, "highTime", now - 60);
		set(latest, "lowTime", now - 60);

		PriceWindow oneHour = new PriceWindow();
		set(oneHour, "avgHighPrice", high);
		set(oneHour, "avgLowPrice", low);
		set(oneHour, "highPriceVolume", highVolume);
		set(oneHour, "lowPriceVolume", lowVolume);

		PriceWindow fiveMinute = new PriceWindow();
		set(fiveMinute, "avgHighPrice", high);
		set(fiveMinute, "avgLowPrice", low);
		set(fiveMinute, "highPriceVolume", hourlyVolume / 24);
		set(fiveMinute, "lowPriceVolume", hourlyVolume / 24);

		return new MarketItem(metadata, latest, fiveMinute, oneHour);
	}

	private void set(Object target, String fieldName, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
