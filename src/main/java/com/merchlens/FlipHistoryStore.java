package com.merchlens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merchlens.model.FlipHistorySummary;
import com.merchlens.model.FlipRecord;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.runelite.client.config.ConfigManager;

class FlipHistoryStore
{
	private static final String GROUP = "merchlens";
	private static final String KEY = "flipHistory";
	private static final int MAX_RECORDS = 300;
	private static final Type RECORDS_TYPE = new TypeToken<List<FlipRecord>>() {}.getType();

	private final ConfigManager configManager;
	private final Gson gson;
	private final List<FlipRecord> records = new ArrayList<>();

	FlipHistoryStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	void load()
	{
		records.clear();
		if (configManager == null)
		{
			return;
		}
		String json = configManager.getConfiguration(GROUP, KEY);
		if (json == null || json.trim().isEmpty())
		{
			return;
		}
		List<FlipRecord> loaded = gson.fromJson(json, RECORDS_TYPE);
		if (loaded != null)
		{
			records.addAll(loaded);
		}
	}

	void record(OfferSnapshot snapshot)
	{
		if (snapshot == null || snapshot.getItemId() <= 0)
		{
			return;
		}
		String state = snapshot.getState();
		if ("BOUGHT".equals(state))
		{
			recordBuy(snapshot);
			save();
		}
		else if ("SOLD".equals(state))
		{
			recordSell(snapshot);
			save();
		}
	}

	List<FlipRecord> openFlips()
	{
		List<FlipRecord> open = new ArrayList<>();
		for (FlipRecord record : records)
		{
			if (record.isOpen() && !record.isExcluded())
			{
				open.add(record);
			}
		}
		open.sort(Comparator.comparingLong(FlipRecord::getBoughtAt).reversed());
		return open;
	}

	List<FlipRecord> recentClosed()
	{
		List<FlipRecord> closed = new ArrayList<>();
		for (FlipRecord record : records)
		{
			if (record.isClosed() && !record.isExcluded())
			{
				closed.add(record);
			}
		}
		closed.sort(Comparator.comparingLong(FlipRecord::getSoldAt).reversed());
		return closed.subList(0, Math.min(10, closed.size()));
	}

	FlipHistorySummary summary()
	{
		int openCount = 0;
		int closedCount = 0;
		int excludedCount = 0;
		int grossProfit = 0;
		int netProfit = 0;
		int totalQuantity = 0;
		for (FlipRecord record : records)
		{
			if (record.isExcluded())
			{
				excludedCount++;
				continue;
			}
			if (record.isOpen())
			{
				openCount++;
			}
			else if (record.isClosed())
			{
				closedCount++;
				grossProfit += record.getGrossProfit();
				netProfit += record.getNetProfit();
				totalQuantity += record.getSellQuantity();
			}
		}
		return new FlipHistorySummary(openCount, closedCount, excludedCount, grossProfit, netProfit, totalQuantity);
	}

	void exclude(String recordId)
	{
		if (recordId == null || recordId.trim().isEmpty())
		{
			return;
		}
		for (FlipRecord record : records)
		{
			if (recordId.equals(record.getId()))
			{
				record.setExcluded(true);
				record.setUpdatedAt(System.currentTimeMillis() / 1000L);
				save();
				return;
			}
		}
	}

	private void recordBuy(OfferSnapshot snapshot)
	{
		FlipRecord existing = newestOpen(snapshot.getItemId());
		if (existing != null && existing.getBuyQuantity() == snapshot.getFilledQuantity() && existing.getBuySpent() == snapshot.getSpent())
		{
			existing.setUpdatedAt(snapshot.getUpdatedAt());
			return;
		}

		FlipRecord record = new FlipRecord();
		record.setId(UUID.randomUUID().toString());
		record.setItemId(snapshot.getItemId());
		record.setBuyPrice(snapshot.getPrice());
		record.setBuyQuantity(snapshot.getFilledQuantity());
		record.setBuySpent(snapshot.getSpent());
		record.setBoughtAt(snapshot.getUpdatedAt());
		record.setState("OPEN");
		record.setUpdatedAt(snapshot.getUpdatedAt());
		records.add(record);
		trim();
	}

	private void recordSell(OfferSnapshot snapshot)
	{
		int remainingQuantity = snapshot.getFilledQuantity();
		int remainingReceived = snapshot.getSpent();
		while (remainingQuantity > 0)
		{
			FlipRecord open = oldestOpen(snapshot.getItemId());
			if (open == null)
			{
				records.add(unmatchedSell(snapshot, remainingQuantity, remainingReceived));
				break;
			}

			int matchedQuantity = Math.min(open.getBuyQuantity(), remainingQuantity);
			int matchedReceived = prorate(remainingReceived, matchedQuantity, remainingQuantity);
			records.add(closeMatchedQuantity(open, snapshot, matchedQuantity, matchedReceived));

			if (matchedQuantity >= open.getBuyQuantity())
			{
				records.remove(open);
			}
			else
			{
				int remainingOpenQuantity = open.getBuyQuantity() - matchedQuantity;
				int remainingOpenSpent = open.getBuySpent() - prorate(open.getBuySpent(), matchedQuantity, open.getBuyQuantity());
				open.setBuyQuantity(remainingOpenQuantity);
				open.setBuySpent(remainingOpenSpent);
				open.setBuyPrice(remainingOpenQuantity <= 0 ? 0 : remainingOpenSpent / remainingOpenQuantity);
				open.setUpdatedAt(snapshot.getUpdatedAt());
			}

			remainingQuantity -= matchedQuantity;
			remainingReceived -= matchedReceived;
		}
		trim();
	}

	private FlipRecord closeMatchedQuantity(FlipRecord open, OfferSnapshot snapshot, int matchedQuantity, int matchedReceived)
	{
		int matchedBuySpent = prorate(open.getBuySpent(), matchedQuantity, open.getBuyQuantity());
		FlipRecord closed = new FlipRecord();
		closed.setId(UUID.randomUUID().toString());
		closed.setItemId(open.getItemId());
		closed.setItemName(open.getItemName());
		closed.setBuyPrice(matchedQuantity <= 0 ? 0 : matchedBuySpent / matchedQuantity);
		closed.setBuyQuantity(matchedQuantity);
		closed.setBuySpent(matchedBuySpent);
		closed.setBoughtAt(open.getBoughtAt());
		closed.setSellPrice(snapshot.getPrice());
		closed.setSellQuantity(matchedQuantity);
		closed.setSellReceived(matchedReceived);
		closed.setSoldAt(snapshot.getUpdatedAt());
		closed.setState("CLOSED");
		closed.setUpdatedAt(snapshot.getUpdatedAt());
		return closed;
	}

	private FlipRecord unmatchedSell(OfferSnapshot snapshot, int quantity, int received)
	{
		FlipRecord record = new FlipRecord();
		record.setId(UUID.randomUUID().toString());
		record.setItemId(snapshot.getItemId());
		record.setBuyPrice(0);
		record.setBuyQuantity(quantity);
		record.setBuySpent(0);
		record.setBoughtAt(snapshot.getUpdatedAt());
		record.setSellPrice(snapshot.getPrice());
		record.setSellQuantity(quantity);
		record.setSellReceived(received);
		record.setSoldAt(snapshot.getUpdatedAt());
		record.setState("CLOSED");
		record.setUpdatedAt(snapshot.getUpdatedAt());
		return record;
	}

	private int prorate(int total, int partQuantity, int totalQuantity)
	{
		if (totalQuantity <= 0)
		{
			return 0;
		}
		return (int) Math.round(total * (partQuantity / (double) totalQuantity));
	}

	private FlipRecord newestOpen(int itemId)
	{
		FlipRecord newest = null;
		for (FlipRecord record : records)
		{
			if (record.isOpen() && record.getItemId() == itemId && (newest == null || record.getBoughtAt() > newest.getBoughtAt()))
			{
				newest = record;
			}
		}
		return newest;
	}

	private FlipRecord oldestOpen(int itemId)
	{
		FlipRecord oldest = null;
		for (FlipRecord record : records)
		{
			if (record.isOpen() && record.getItemId() == itemId && (oldest == null || record.getBoughtAt() < oldest.getBoughtAt()))
			{
				oldest = record;
			}
		}
		return oldest;
	}

	private void trim()
	{
		records.sort(Comparator.comparingLong(FlipRecord::getUpdatedAt).reversed());
		while (records.size() > MAX_RECORDS)
		{
			records.remove(records.size() - 1);
		}
	}

	private void save()
	{
		if (configManager != null)
		{
			configManager.setConfiguration(GROUP, KEY, gson.toJson(records));
		}
	}
}
