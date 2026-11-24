package com.dto.season;

import com.entity.WatchStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SeasonRequest(
        @NotNull(message = "Season number is required")
        @Min(value = 1, message = "Season number must be at least 1")
        Integer seasonNumber,

        String title,

        WatchStatus status,

        @Min(value = 0, message = "Episodes watched cannot be negative")
        Integer episodesWatched,

        @Min(value = 1, message = "Total episodes must be at least 1")
        Integer totalEpisodes,

        @Min(value = 1, message = "Rating must be between 1 and 10")
        @Max(value = 10, message = "Rating must be between 1 and 10")
        Integer rating
) {
}
