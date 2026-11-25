package com.dto.anime;

import com.dto.season.SeasonRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAnimeRequest(
        @NotBlank(message = "Anime title is required")
        @Size(max = 255, message = "Anime title must not exceed 255 characters")
        String title,

        @Size(max = 255, message = "Cover image URL must not exceed 255 characters")
        String coverImageUrl,

        List<@Valid SeasonRequest> seasons
) {
}
