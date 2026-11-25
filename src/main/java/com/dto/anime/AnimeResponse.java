package com.dto.anime;

import com.dto.season.SeasonResponse;
import com.entity.Anime;
import com.entity.Season;
import com.entity.WatchStatus;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AnimeResponse(
        Long id,
        String title,
        String coverImageUrl,
        WatchStatus overallStatus,
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
                deriveOverallStatus(anime.getSeasons()),
                seasonResponses,
                anime.getCreatedAt(),
                anime.getUpdatedAt()
        );
    }

    public static WatchStatus deriveOverallStatus(List<Season> seasons) {
        if (seasons == null || seasons.isEmpty()) {
            return WatchStatus.PLAN_TO_WATCH;
        }

        Set<WatchStatus> statuses = EnumSet.noneOf(WatchStatus.class);
        for (Season season : seasons) {
            if (season != null && season.getStatus() != null) {
                statuses.add(season.getStatus());
            }
        }

        if (statuses.isEmpty()) {
            return WatchStatus.PLAN_TO_WATCH;
        }
        if (statuses.contains(WatchStatus.WATCHING)) {
            return WatchStatus.WATCHING;
        }
        if (statuses.contains(WatchStatus.ON_HOLD)) {
            return WatchStatus.ON_HOLD;
        }
        if (statuses.size() == 1) {
            return statuses.iterator().next();
        }
        if (statuses.contains(WatchStatus.COMPLETED) && statuses.contains(WatchStatus.PLAN_TO_WATCH)) {
            return WatchStatus.WATCHING;
        }
        if (statuses.contains(WatchStatus.PLAN_TO_WATCH)) {
            return WatchStatus.PLAN_TO_WATCH;
        }
        return WatchStatus.DROPPED;
    }
}
