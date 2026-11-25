package com.dto.discover;

public record DiscoverAnimeResponse(
        Long anilistId,
        String title,
        String coverImageUrl,
        Integer episodes,
        Double score,
        String airingStatus,
        String synopsis
) {
}
