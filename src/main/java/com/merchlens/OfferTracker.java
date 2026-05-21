package com.merchlens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.client.config.ConfigManager;

class OfferTracker
{
	private static final String GROUP = "merchlens";
	private static final String KEY = "trackedOffers";
	private static final long STARTUP_UNKNOWN_WINDOW_SECONDS = 20;
	private static final Type TRACKED_OFFERS_TYPE = new TypeToken<List<TrackedOffer>>() {}.getType();

	private final List<OfferSnapshot> snapshots = new ArrayList<>();
	private final Map<Integer, TrackedOffer> activeOffers = new HashMap<>();
	private final ConfigManager configManager;
	private final Gson gson;
	private final long startedAt = Instant.now().getEpochSecond();

	OfferTracker(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	OfferSnapshot record(GrandExchangeOffer offer)
	{
		return record(-1, offer);
	}

	synchronized OfferSnapshot record(int slot, GrandExchangeOffer offer)
	{
		if (offer == null || offer.getItemId() <= 0)
		{
			if (slot >= 0)
			{
				activeOffers.remove(slot);
				save();
			}
			return null;
		}
		long now = Instant.now().getEpochSecond();
		OfferSnapshot snapshot = new OfferSnapshot(slot, offer, now);
		snapshots.add(snapshot);
		while (snapshots.size() > 128)
		{
			snapshots.remove(0);
		}
		if (slot >= 0)
		{
			recordActiveOffer(slot, offer, now);
		}
		return snapshot;
	}

	synchronized List<OfferSnapshot> recent()
	{
		return Collections.unmodifiableList(new ArrayList<>(snapshots));
	}

	synchronized List<TrackedOffer> activeOffers()
	{
		List<TrackedOffer> offers = new ArrayList<>();
		for (TrackedOffer offer : activeOffers.values())
		{
			if (offer != null && offer.isVisible())
			{
				offers.add(offer);
			}
		}
		offers.sort(Comparator.comparingInt(TrackedOffer::getSlot));
		return offers;
	}

	synchronized void load()
	{
		activeOffers.clear();
		if (configManager == null || gson == null)
		{
			return;
		}
		String json = configManager.getConfiguration(GROUP, KEY);
		if (json == null || json.trim().isEmpty())
		{
			return;
		}
		List<TrackedOffer> loaded = gson.fromJson(json, TRACKED_OFFERS_TYPE);
		if (loaded == null)
		{
			return;
		}
		for (TrackedOffer offer : loaded)
		{
			if (offer != null && offer.isVisible() && offer.getSlot() >= 0)
			{
				activeOffers.put(offer.getSlot(), offer);
			}
		}
	}

	synchronized void clear()
	{
		snapshots.clear();
	}

	private void recordActiveOffer(int slot, GrandExchangeOffer offer, long now)
	{
		String state = offer.getState() == null ? "EMPTY" : offer.getState().name();
		if ("EMPTY".equals(state) || offer.getItemId() <= 0 || offer.getTotalQuantity() <= 0)
		{
			activeOffers.remove(slot);
			save();
			return;
		}

		TrackedOffer tracked = activeOffers.get(slot);
		if (tracked != null && tracked.matches(offer))
		{
			tracked.update(offer, now);
		}
		else
		{
			boolean knownStart = now - startedAt > STARTUP_UNKNOWN_WINDOW_SECONDS;
			tracked = new TrackedOffer(slot, offer, now, knownStart);
			activeOffers.put(slot, tracked);
		}
		save();
	}

	private void save()
	{
		if (configManager != null && gson != null)
		{
			configManager.setConfiguration(GROUP, KEY, gson.toJson(activeOffers()));
		}
	}
}
