package com.merchlens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.config.ConfigManager;

class FavoriteItemStore
{
	private static final String GROUP = "merchlens";
	private static final String KEY = "favoriteItems";
	private static final Type IDS_TYPE = new TypeToken<List<Integer>>() {}.getType();

	private final ConfigManager configManager;
	private final Gson gson;
	private final LinkedHashSet<Integer> itemIds = new LinkedHashSet<>();

	FavoriteItemStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	void load()
	{
		itemIds.clear();
		if (configManager == null)
		{
			return;
		}
		String json = configManager.getConfiguration(GROUP, KEY);
		if (json == null || json.trim().isEmpty())
		{
			return;
		}
		List<Integer> loaded = gson.fromJson(json, IDS_TYPE);
		if (loaded != null)
		{
			for (Integer itemId : loaded)
			{
				if (itemId != null && itemId > 0)
				{
					itemIds.add(itemId);
				}
			}
		}
	}

	Set<Integer> itemIds()
	{
		return new LinkedHashSet<>(itemIds);
	}

	void toggle(int itemId)
	{
		if (itemId <= 0)
		{
			return;
		}
		if (!itemIds.remove(itemId))
		{
			itemIds.add(itemId);
		}
		save();
	}

	private void save()
	{
		if (configManager != null)
		{
			configManager.setConfiguration(GROUP, KEY, gson.toJson(new ArrayList<>(itemIds)));
		}
	}
}
