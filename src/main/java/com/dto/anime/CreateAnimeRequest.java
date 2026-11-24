package com.dto.anime;

import com.dto.season.SeasonRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateAnimeRequest(
        @NotNull(message = "Anime title is required")
        String title,

        String coverImageUrl,

        @Valid
        List<SeasonRequest> seasons
) {
}
