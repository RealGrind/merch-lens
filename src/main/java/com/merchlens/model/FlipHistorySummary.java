package com.merchlens.model;

public class FlipHistorySummary
{
	private final int openCount;
	private final int closedCount;
	private final int excludedCount;
	private final int grossProfit;
	private final int netProfit;
	private final int totalQuantity;

	public FlipHistorySummary(int openCount, int closedCount, int excludedCount, int grossProfit, int netProfit, int totalQuantity)
	{
		this.openCount = openCount;
		this.closedCount = closedCount;
		this.excludedCount = excludedCount;
		this.grossProfit = grossProfit;
		this.netProfit = netProfit;
		this.totalQuantity = totalQuantity;
	}

	public int getOpenCount()
	{
		return openCount;
	}

	public int getClosedCount()
	{
		return closedCount;
	}

	public int getExcludedCount()
	{
		return excludedCount;
	}

	public int getGrossProfit()
	{
		return grossProfit;
	}

	public int getNetProfit()
	{
		return netProfit;
	}

	public int getTotalQuantity()
	{
		return totalQuantity;
	}
}
