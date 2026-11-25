package com.dto.anime;

import jakarta.validation.constraints.Size;

public record UpdateAnimeRequest(
        @Size(max = 255, message = "Anime title must not exceed 255 characters")
        String title,

        @Size(max = 255, message = "Cover image URL must not exceed 255 characters")
        String coverImageUrl
) {
}
