package com.merchlens;

import com.google.gson.Gson;
import com.merchlens.model.FlipRecord;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.runelite.client.config.ConfigManager;

class FlipLogStore
{
	private static final String GROUP = "merchlens";
	private static final String KEY = "flipLogState";

	private final ConfigManager configManager;
	private final Gson gson;
	private State state = new State();

	FlipLogStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	synchronized void load()
	{
		state = new State();
		if (configManager == null || gson == null)
		{
			return;
		}
		String json = configManager.getConfiguration(GROUP, KEY);
		if (json == null || json.trim().isEmpty())
		{
			return;
		}
		State loaded = gson.fromJson(json, State.class);
		if (loaded != null)
		{
			state = loaded;
			state.ensureCollections();
		}
	}

	synchronized void record(OfferFill fill, String itemName)
	{
		if (fill == null || fill.getItemId() <= 0 || fill.getQuantity() <= 0 || fill.getTotalValue() < 0)
		{
			return;
		}
		state.ensureCollections();
		String name = itemName == null || itemName.trim().isEmpty() ? "Item " + fill.getItemId() : itemName;
		if (fill.getSide() == OfferFill.Side.BUY)
		{
			state.buyLots.add(new BuyLot(fill.getItemId(), name, fill.getQuantity(), fill.getTotalValue(), fill.getFilledAt()));
			save();
			return;
		}
		realizeSale(fill, name);
		save();
	}

	synchronized List<FlipRecord> records()
	{
		state.ensureCollections();
		return new ArrayList<>(state.records);
	}

	synchronized void clearCompletedSince(long since)
	{
		state.ensureCollections();
		state.records.removeIf(record -> record != null && record.getCompletedAt() >= since);
		save();
	}

	private void realizeSale(OfferFill fill, String itemName)
	{
		long remainingSaleQuantity = fill.getQuantity();
		long matchedQuantity = 0;
		long matchedCost = 0;
		for (BuyLot lot : state.buyLots)
		{
			if (remainingSaleQuantity <= 0)
			{
				break;
			}
			if (lot == null || lot.itemId != fill.getItemId() || lot.remainingQuantity <= 0)
			{
				continue;
			}
			long quantity = Math.min(remainingSaleQuantity, lot.remainingQuantity);
			long cost = allocatedValue(lot.remainingCost, lot.remainingQuantity, quantity);
			lot.remainingQuantity -= quantity;
			lot.remainingCost -= cost;
			remainingSaleQuantity -= quantity;
			matchedQuantity += quantity;
			matchedCost += cost;
		}
		for (Iterator<BuyLot> iterator = state.buyLots.iterator(); iterator.hasNext();)
		{
			BuyLot lot = iterator.next();
			if (lot == null || lot.remainingQuantity <= 0)
			{
				iterator.remove();
			}
		}
		if (matchedQuantity <= 0)
		{
			return;
		}
		long grossProceeds = allocatedValue(fill.getTotalValue(), fill.getQuantity(), matchedQuantity);
		long averageSellPrice = Math.round(grossProceeds / (double) matchedQuantity);
		int taxedPrice = (int) Math.min(Integer.MAX_VALUE, Math.max(0, averageSellPrice));
		long tax = (long) GeTax.tax(taxedPrice, itemName) * matchedQuantity;
		state.records.add(new FlipRecord(
			fill.getItemId(),
			itemName,
			matchedQuantity,
			matchedCost,
			grossProceeds,
			tax,
			fill.getFilledAt(),
			fill.getOfferKey()
		));
	}

	private long allocatedValue(long totalValue, long totalQuantity, long allocatedQuantity)
	{
		if (allocatedQuantity >= totalQuantity)
		{
			return totalValue;
		}
		return Math.round(totalValue * (allocatedQuantity / (double) totalQuantity));
	}

	private void save()
	{
		if (configManager != null && gson != null)
		{
			configManager.setConfiguration(GROUP, KEY, gson.toJson(state));
		}
	}

	private static class State
	{
		private List<BuyLot> buyLots = new ArrayList<>();
		private List<FlipRecord> records = new ArrayList<>();

		private void ensureCollections()
		{
			if (buyLots == null)
			{
				buyLots = new ArrayList<>();
			}
			if (records == null)
			{
				records = new ArrayList<>();
			}
		}
	}

	private static class BuyLot
	{
		private int itemId;
		private String itemName;
		private long remainingQuantity;
		private long remainingCost;
		private long boughtAt;

		private BuyLot()
		{
		}

		private BuyLot(int itemId, String itemName, long quantity, long cost, long boughtAt)
		{
			this.itemId = itemId;
			this.itemName = itemName;
			this.remainingQuantity = quantity;
			this.remainingCost = cost;
			this.boughtAt = boughtAt;
		}
	}
}
