package com.dto.anime;

public record UpdateAnimeRequest(
        String title,
        String coverImageUrl
) {
}
