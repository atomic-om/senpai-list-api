package com.dto;

import com.entity.WatchStatus;

import java.util.Map;

public record StatsResponse(
        long totalAnimes,
        long totalSeasons,
        long totalEpisodesWatched,
        Double averageRating,
        Map<WatchStatus, Long> seasonsByStatus
) {
}
