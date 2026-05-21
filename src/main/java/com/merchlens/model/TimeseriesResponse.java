package com.merchlens.model;

import java.util.Collections;
import java.util.List;

public class TimeseriesResponse
{
	private List<TimeseriesPoint> data;

	public List<TimeseriesPoint> getData()
	{
		return data == null ? Collections.emptyList() : data;
	}
}
