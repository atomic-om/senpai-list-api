package com.dto.season;

import com.entity.Season;
import com.entity.WatchStatus;

public record SeasonResponse(
        Long id,
        Integer seasonNumber,
        String title,
        WatchStatus status,
        Integer episodesWatched,
        Integer totalEpisodes,
        Integer rating
) {
    public static SeasonResponse from(Season season) {
        return new SeasonResponse(
                season.getId(),
                season.getSeasonNumber(),
                season.getTitle(),
                season.getStatus(),
                season.getEpisodesWatched(),
                season.getTotalEpisodes(),
                season.getRating()
        );
    }
}
