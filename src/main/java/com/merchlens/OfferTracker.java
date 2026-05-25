package com.merchlens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
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

	private final Map<Integer, TrackedOffer> activeOffers = new HashMap<>();
	private final ConfigManager configManager;
	private final Gson gson;
	private final long startedAt = Instant.now().getEpochSecond();

	OfferTracker(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	synchronized OfferFill record(int slot, GrandExchangeOffer offer)
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
		if (slot >= 0)
		{
			return recordActiveOffer(slot, offer, now);
		}
		return null;
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

	private OfferFill recordActiveOffer(int slot, GrandExchangeOffer offer, long now)
	{
		String state = offer.getState() == null ? "EMPTY" : offer.getState().name();
		if ("EMPTY".equals(state) || offer.getItemId() <= 0 || offer.getTotalQuantity() <= 0)
		{
			activeOffers.remove(slot);
			save();
			return null;
		}

		TrackedOffer tracked = activeOffers.get(slot);
		boolean matchingOffer = tracked != null && tracked.matches(offer);
		int previousQuantity = matchingOffer ? tracked.getFilledQuantity() : 0;
		int previousSpent = matchingOffer ? tracked.getSpent() : 0;
		boolean captureExistingProgress = matchingOffer;
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
		int filledQuantity = offer.getQuantitySold() - previousQuantity;
		long filledValue = (long) offer.getSpent() - previousSpent;
		boolean captureNewOffer = !matchingOffer && tracked.isTrackedStartKnown();
		if (filledQuantity <= 0 || filledValue < 0 || (!captureExistingProgress && !captureNewOffer))
		{
			return null;
		}
		OfferFill.Side side = tracked.isBuyOffer() ? OfferFill.Side.BUY : OfferFill.Side.SELL;
		return new OfferFill(side, offer.getItemId(), filledQuantity, filledValue, now, tracked.offerKey());
	}

	private void save()
	{
		if (configManager != null && gson != null)
		{
			configManager.setConfiguration(GROUP, KEY, gson.toJson(activeOffers()));
		}
	}
}
