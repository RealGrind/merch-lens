package com.merchlens.model;

public class ItemMetadata
{
	private int id;
	private String name;
	private boolean members;
	private int limit;
	private int highalch;
	private int value;
	private String icon;

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public boolean isMembers()
	{
		return members;
	}

	public int getLimit()
	{
		return Math.max(limit, 1);
	}

	public int getHighalch()
	{
		return highalch;
	}

	public int getValue()
	{
		return value;
	}

	public String getIcon()
	{
		return icon;
	}
}
