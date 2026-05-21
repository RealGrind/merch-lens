package com.merchlens.model;

public class ItemSearchResult
{
	private final int itemId;
	private final String itemName;

	public ItemSearchResult(int itemId, String itemName)
	{
		this.itemId = itemId;
		this.itemName = itemName;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getItemName()
	{
		return itemName;
	}
}
