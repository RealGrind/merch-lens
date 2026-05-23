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

	public SignalResponse(
		long generatedAt,
		List<RecommendationDto> recommendations,
		List<RecommendationDto> highVolumeRecommendations,
		List<RecommendationDto> favoriteRecommendations,
		Set<Integer> favoriteItemIds)
	{
		this.generatedAt = generatedAt;
		this.recommendations = recommendations;
		this.highVolumeRecommendations = highVolumeRecommendations;
		this.favoriteRecommendations = favoriteRecommendations;
		this.favoriteItemIds = favoriteItemIds;
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
}
