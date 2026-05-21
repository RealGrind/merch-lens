package com.merchlens.model;

import java.util.Collections;
import java.util.Map;

public class WikiEnvelope<T>
{
	private long timestamp;
	private Map<Integer, T> data;

	public long getTimestamp()
	{
		return timestamp;
	}

	public Map<Integer, T> getData()
	{
		return data == null ? Collections.emptyMap() : data;
	}
}
