package com.merchlens;

import com.merchlens.model.HistoricalSignal;
import com.merchlens.model.TimeseriesPoint;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class HistoricalSignalAnalyzerTest
{
	private final HistoricalSignalAnalyzer analyzer = new HistoricalSignalAnalyzer();

	@Test
	public void stableHistoryProducesStableSignal() throws Exception
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 288; i++)
		{
			points.add(point(20, 18, 400, 400));
		}

		HistoricalSignal signal = analyzer.analyze(1, 18, 20, points);

		Assert.assertEquals(HistoricalSignal.MarketState.STABLE, signal.getMarketState());
		Assert.assertTrue(Math.abs(signal.getDeviationFromMedian()) < 0.01);
	}

	@Test
	public void elevatedCurrentPriceProducesSpikeSignal() throws Exception
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 288; i++)
		{
			points.add(point(20, 18, 400, 400));
		}

		HistoricalSignal signal = analyzer.analyze(1, 28, 32, points);

		Assert.assertEquals(HistoricalSignal.MarketState.SPIKING, signal.getMarketState());
		Assert.assertTrue(signal.getWarning().contains("elevated"));
	}

	@Test
	public void recentDowntrendProducesFallingSignal() throws Exception
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 240; i++)
		{
			points.add(point(102, 98, 400, 400));
		}
		for (int i = 0; i < 48; i++)
		{
			points.add(point(92, 88, 400, 400));
		}

		HistoricalSignal signal = analyzer.analyze(1, 88, 92, points);

		Assert.assertTrue(signal.getMarketState() == HistoricalSignal.MarketState.FALLING
			|| signal.getMarketState() == HistoricalSignal.MarketState.CRASHING);
	}

	@Test
	public void repeatingLowAndHighBandsProduceRangeBoundSignal() throws Exception
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 72; i++)
		{
			points.add(point(5, 3, 400, 400));
			points.add(point(10, 8, 400, 400));
			points.add(point(23, 21, 400, 400));
			points.add(point(11, 9, 400, 400));
		}

		HistoricalSignal signal = analyzer.analyze(1, 2, 8, points);

		Assert.assertEquals(HistoricalSignal.MarketState.RANGE_BOUND, signal.getMarketState());
	}

	@Test
	public void upperBandOfRepeatingChannelProducesSpikeSignal() throws Exception
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 72; i++)
		{
			points.add(point(5, 3, 400, 400));
			points.add(point(10, 8, 400, 400));
			points.add(point(23, 21, 400, 400));
			points.add(point(11, 9, 400, 400));
		}

		HistoricalSignal signal = analyzer.analyze(1, 20, 24, points);

		Assert.assertEquals(HistoricalSignal.MarketState.SPIKING, signal.getMarketState());
	}

	@Test
	public void lowPricedRepeatedFloorAndCeilingProducesRangeBoundSignal() throws Exception
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 58; i++)
		{
			points.add(point(10, 2, 120, 120));
			points.add(point(10, 3, 120, 120));
			points.add(point(6, 3, 120, 120));
			points.add(point(10, 4, 120, 120));
			points.add(point(5, 2, 120, 120));
		}

		HistoricalSignal signal = analyzer.analyze(1, 4, 10, points);

		Assert.assertEquals(HistoricalSignal.MarketState.RANGE_BOUND, signal.getMarketState());
	}

	@Test
	public void repeatedSellCeilingWithNormalBuyFloorIsNotSpike() throws Exception
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 96; i++)
		{
			points.add(point(299, 290, 5_000, 5_000));
			points.add(point(300, 290, 5_000, 5_000));
			points.add(point(298, 289, 5_000, 5_000));
		}

		HistoricalSignal signal = analyzer.analyze(1, 290, 300, points);

		Assert.assertNotEquals(HistoricalSignal.MarketState.SPIKING, signal.getMarketState());
		Assert.assertTrue(signal.getMarketState() == HistoricalSignal.MarketState.STABLE
			|| signal.getMarketState() == HistoricalSignal.MarketState.RANGE_BOUND);
	}

	@Test
	public void risingFloorAndCeilingDoesNotProduceStableSignal() throws Exception
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 144; i++)
		{
			points.add(point(11_550, 11_320, 400, 400));
		}
		for (int i = 0; i < 144; i++)
		{
			points.add(point(11_850, 11_560, 400, 400));
		}

		HistoricalSignal signal = analyzer.analyze(1, 11_560, 11_700, points);

		Assert.assertEquals(HistoricalSignal.MarketState.RISING, signal.getMarketState());
	}

	private TimeseriesPoint point(int high, int low, int highVolume, int lowVolume) throws Exception
	{
		TimeseriesPoint point = new TimeseriesPoint();
		set(point, "avgHighPrice", high);
		set(point, "avgLowPrice", low);
		set(point, "highPriceVolume", highVolume);
		set(point, "lowPriceVolume", lowVolume);
		return point;
	}

	private void set(Object target, String fieldName, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
