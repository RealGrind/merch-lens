package com.merchlens.model;

import com.merchlens.GeTax;

public class FlipRecord
{
	private String id;
	private int itemId;
	private String itemName;
	private int buyPrice;
	private int buyQuantity;
	private int buySpent;
	private long boughtAt;
	private int sellPrice;
	private int sellQuantity;
	private int sellReceived;
	private long soldAt;
	private String state;
	private boolean excluded;
	private long updatedAt;

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public int getItemId()
	{
		return itemId;
	}

	public void setItemId(int itemId)
	{
		this.itemId = itemId;
	}

	public String getItemName()
	{
		return itemName;
	}

	public void setItemName(String itemName)
	{
		this.itemName = itemName;
	}

	public int getBuyPrice()
	{
		return buyPrice;
	}

	public void setBuyPrice(int buyPrice)
	{
		this.buyPrice = buyPrice;
	}

	public int getBuyQuantity()
	{
		return buyQuantity;
	}

	public void setBuyQuantity(int buyQuantity)
	{
		this.buyQuantity = buyQuantity;
	}

	public int getBuySpent()
	{
		return buySpent;
	}

	public void setBuySpent(int buySpent)
	{
		this.buySpent = buySpent;
	}

	public long getBoughtAt()
	{
		return boughtAt;
	}

	public void setBoughtAt(long boughtAt)
	{
		this.boughtAt = boughtAt;
	}

	public int getSellPrice()
	{
		return sellPrice;
	}

	public void setSellPrice(int sellPrice)
	{
		this.sellPrice = sellPrice;
	}

	public int getSellQuantity()
	{
		return sellQuantity;
	}

	public void setSellQuantity(int sellQuantity)
	{
		this.sellQuantity = sellQuantity;
	}

	public int getSellReceived()
	{
		return sellReceived;
	}

	public void setSellReceived(int sellReceived)
	{
		this.sellReceived = sellReceived;
	}

	public long getSoldAt()
	{
		return soldAt;
	}

	public void setSoldAt(long soldAt)
	{
		this.soldAt = soldAt;
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	public boolean isExcluded()
	{
		return excluded;
	}

	public void setExcluded(boolean excluded)
	{
		this.excluded = excluded;
	}

	public long getUpdatedAt()
	{
		return updatedAt;
	}

	public void setUpdatedAt(long updatedAt)
	{
		this.updatedAt = updatedAt;
	}

	public boolean isOpen()
	{
		return "OPEN".equals(state);
	}

	public boolean isClosed()
	{
		return "CLOSED".equals(state);
	}

	public int getAverageBuyPrice()
	{
		return buyQuantity <= 0 || buySpent <= 0 ? buyPrice : buySpent / buyQuantity;
	}

	public int getAverageSellPrice()
	{
		return sellQuantity <= 0 || sellReceived <= 0 ? sellPrice : sellReceived / sellQuantity;
	}

	public int getGrossProfit()
	{
		return sellReceived - buySpent;
	}

	public int getEstimatedTax()
	{
		if (sellQuantity <= 0)
		{
			return 0;
		}
		return GeTax.tax(getAverageSellPrice(), itemName) * sellQuantity;
	}

	public int getNetProfit()
	{
		return getGrossProfit() - getEstimatedTax();
	}

	public double getRoi()
	{
		return buySpent <= 0 ? 0 : getNetProfit() / (double) buySpent;
	}
}
