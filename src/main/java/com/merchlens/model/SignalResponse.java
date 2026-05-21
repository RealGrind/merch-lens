package com.merchlens.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SignalResponse
{
	private final long generatedAt;
	private final List<RecommendationDto> recommendations;
	private final List<RecommendationDto> highVolumeRecommendations;
	private final List<RecommendationDto> favoriteRecommendations;
	private final Set<Integer> favoriteItemIds;
	private final List<OfferAdvice> offerAdvice;
	private final FlipHistorySummary flipHistorySummary;
	private final List<FlipRecord> recentClosedFlips;

	public SignalResponse(
		long generatedAt,
		List<RecommendationDto> recommendations,
		List<RecommendationDto> highVolumeRecommendations,
		List<RecommendationDto> favoriteRecommendations,
		Set<Integer> favoriteItemIds,
		List<OfferAdvice> offerAdvice,
		FlipHistorySummary flipHistorySummary,
		List<FlipRecord> recentClosedFlips)
	{
		this.generatedAt = generatedAt;
		this.recommendations = recommendations;
		this.highVolumeRecommendations = highVolumeRecommendations;
		this.favoriteRecommendations = favoriteRecommendations;
		this.favoriteItemIds = favoriteItemIds;
		this.offerAdvice = offerAdvice;
		this.flipHistorySummary = flipHistorySummary;
		this.recentClosedFlips = recentClosedFlips;
	}

	public long getGeneratedAt()
	{
		return generatedAt;
	}

	public List<RecommendationDto> getRecommendations()
	{
		return recommendations == null ? Collections.emptyList() : recommendations;
	}

	public List<RecommendationDto> getHighVolumeRecommendations()
	{
		return highVolumeRecommendations == null ? Collections.emptyList() : highVolumeRecommendations;
	}

	public List<RecommendationDto> getFavoriteRecommendations()
	{
		return favoriteRecommendations == null ? Collections.emptyList() : favoriteRecommendations;
	}

	public Set<Integer> getFavoriteItemIds()
	{
		return favoriteItemIds == null ? Collections.emptySet() : favoriteItemIds;
	}

	public List<OfferAdvice> getOfferAdvice()
	{
		return offerAdvice == null ? Collections.emptyList() : offerAdvice;
	}

	public FlipHistorySummary getFlipHistorySummary()
	{
		return flipHistorySummary == null ? new FlipHistorySummary(0, 0, 0, 0, 0, 0) : flipHistorySummary;
	}

	public List<FlipRecord> getRecentClosedFlips()
	{
		return recentClosedFlips == null ? Collections.emptyList() : recentClosedFlips;
	}
}
