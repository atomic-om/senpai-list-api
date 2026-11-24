package com.dto.anime;

import com.dto.season.SeasonResponse;
import com.entity.Anime;

import java.time.LocalDateTime;
import java.util.List;

public record AnimeResponse(
        Long id,
        String title,
        String coverImageUrl,
        List<SeasonResponse> seasons,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnimeResponse from(Anime anime) {
        List<SeasonResponse> seasonResponses = anime.getSeasons().stream()
                .map(SeasonResponse::from)
                .toList();

        return new AnimeResponse(
                anime.getId(),
                anime.getTitle(),
                anime.getCoverImageUrl(),
                seasonResponses,
                anime.getCreatedAt(),
                anime.getUpdatedAt()
        );
    }
}
