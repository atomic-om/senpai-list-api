package com.service;

import com.dto.StatsResponse;
import com.dto.anime.AnimeResponse;
import com.dto.anime.CreateAnimeRequest;
import com.dto.anime.UpdateAnimeRequest;
import com.dto.season.SeasonRequest;
import com.entity.Anime;
import com.entity.Season;
import com.entity.User;
import com.entity.WatchStatus;
import com.exception.BadRequestException;
import com.exception.DuplicateResourceException;
import com.exception.ResourceNotFoundException;
import com.repository.AnimeRepository;
import com.repository.AnimeSpecification;
import com.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnimeService {

    private final AnimeRepository animeRepository;
    private final SeasonRepository seasonRepository;

    @Transactional
    public AnimeResponse addAnime(CreateAnimeRequest request, User currentUser) {
        String trimmedTitle = request.title().trim();

        if (animeRepository.existsByUserIdAndTitleIgnoreCase(currentUser.getId(), trimmedTitle)) {
            throw new DuplicateResourceException("Anime '" + trimmedTitle + "' is already in your watchlist");
        }

        Anime anime = Anime.builder()
                .title(trimmedTitle)
                .coverImageUrl(request.coverImageUrl())
                .user(currentUser)
                .build();

        if (request.seasons() != null && !request.seasons().isEmpty()) {
            Set<Integer> seenSeasonNumbers = new HashSet<>();

            for (SeasonRequest seasonReq : request.seasons()) {
                if (!seenSeasonNumbers.add(seasonReq.seasonNumber())) {
                    throw new DuplicateResourceException("Duplicate season number: " + seasonReq.seasonNumber());
                }
                int watched = seasonReq.episodesWatched() != null ? seasonReq.episodesWatched() : 0;
                SeasonService.validateEpisodeCounts(watched, seasonReq.totalEpisodes());

                Season season = Season.builder()
                        .seasonNumber(seasonReq.seasonNumber())
                        .title(seasonReq.title())
                        .status(seasonReq.status() != null ? seasonReq.status() : WatchStatus.PLAN_TO_WATCH)
                        .episodesWatched(watched)
                        .totalEpisodes(seasonReq.totalEpisodes())
                        .rating(seasonReq.rating())
                        .build();

                anime.addSeason(season);
            }
        }
        return AnimeResponse.from(animeRepository.save(anime));
    }

    @Transactional(readOnly = true)
    public Page<AnimeResponse> allAnime(User currentUser, String search, WatchStatus status, Pageable pageable) {
        Specification<Anime> spec = AnimeSpecification.filterWatchlist(currentUser.getId(), search, status);
        return animeRepository.findAll(spec, pageable)
                .map(AnimeResponse::from);
    }

    @Transactional(readOnly = true)
    public AnimeResponse findAnime(Long animeId, User currentUser) {
        return AnimeResponse.from(fetchAnime(animeId, currentUser.getId()));
    }

    @Transactional
    public AnimeResponse updateAnime(Long animeId, UpdateAnimeRequest request, User currentUser) {
        Anime anime = fetchAnime(animeId, currentUser.getId());

        if (request.title() != null) {
            String trimmedTitle = request.title().trim();
            if (trimmedTitle.isEmpty()) {
                throw new BadRequestException("Anime title cannot be blank");
            }

            if (!anime.getTitle().equalsIgnoreCase(trimmedTitle)
                    && animeRepository.existsByUserIdAndTitleIgnoreCase(currentUser.getId(), trimmedTitle)) {
                throw new DuplicateResourceException("Anime '" + trimmedTitle + "' is already in your watchlist");
            }
            anime.setTitle(trimmedTitle);
        }

        if (request.coverImageUrl() != null) {
            anime.setCoverImageUrl(request.coverImageUrl());
        }

        return AnimeResponse.from(animeRepository.save(anime));
    }

    @Transactional
    public void deleteAnime(Long animeId, User currentUser) {
        animeRepository.delete(fetchAnime(animeId, currentUser.getId()));
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(User currentUser) {
        Long userId = currentUser.getId();

        long totalAnimes = animeRepository.countByUserId(userId);
        long totalSeasons = seasonRepository.countByUserId(userId);
        long totalEpisodesWatched = seasonRepository.sumEpisodesWatchedByUserId(userId);
        Double rawAvg = seasonRepository.findAverageRatingByUserId(userId);
        Double averageRating = rawAvg != null ? Math.round(rawAvg * 100.0) / 100.0 : null;

        Map<WatchStatus, Long> seasonsByStatus = new EnumMap<>(WatchStatus.class);
        for (WatchStatus status : WatchStatus.values()) {
            seasonsByStatus.put(status, 0L);
        }
        for (Object[] row : seasonRepository.countSeasonsByStatusForUser(userId)) {
            WatchStatus status = (WatchStatus) row[0];
            Long count = (Long) row[1];
            seasonsByStatus.put(status, count);
        }

        return new StatsResponse(
                totalAnimes,
                totalSeasons,
                totalEpisodesWatched,
                averageRating,
                seasonsByStatus
        );
    }

    public Anime fetchAnime(Long animeId, Long userId) {
        return animeRepository.findByIdAndUserId(animeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found with id: " + animeId));
    }
}
