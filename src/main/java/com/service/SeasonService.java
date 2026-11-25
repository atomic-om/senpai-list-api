package com.service;

import com.dto.season.SeasonRequest;
import com.dto.season.SeasonResponse;
import com.dto.season.UpdateSeasonRequest;
import com.entity.Anime;
import com.entity.Season;
import com.entity.User;
import com.entity.WatchStatus;
import com.exception.BadRequestException;
import com.exception.DuplicateResourceException;
import com.exception.ResourceNotFoundException;
import com.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final AnimeService animeService;

    @Transactional(readOnly = true)
    public List<SeasonResponse> allSeason(Long animeId, User currentUser) {
        Anime anime = animeService.fetchAnime(animeId, currentUser.getId());

        return anime.getSeasons().stream()
                .sorted(Comparator.comparing(Season::getSeasonNumber))
                .map(SeasonResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeasonResponse findSeason(Long animeId, Long seasonId, User currentUser) {
        return SeasonResponse.from(fetchSeason(seasonId, animeId, currentUser.getId()));
    }

    @Transactional
    public SeasonResponse addSeason(Long animeId, SeasonRequest request, User currentUser) {
        Anime anime = animeService.fetchAnime(animeId, currentUser.getId());

        if (seasonRepository.existsByAnimeIdAndSeasonNumber(animeId, request.seasonNumber())) {
            throw new DuplicateResourceException("Season " + request.seasonNumber() + " already exists for this anime");
        }

        int watched = request.episodesWatched() != null ? request.episodesWatched() : 0;
        validateEpisodeCounts(watched, request.totalEpisodes());

        Season season = Season.builder()
                .seasonNumber(request.seasonNumber())
                .title(request.title())
                .status(request.status() != null ? request.status() : WatchStatus.PLAN_TO_WATCH)
                .episodesWatched(watched)
                .totalEpisodes(request.totalEpisodes())
                .rating(request.rating())
                .anime(anime)
                .build();

        anime.touch();
        return SeasonResponse.from(seasonRepository.save(season));
    }

    @Transactional
    public SeasonResponse updateSeason(Long animeId, Long seasonId, UpdateSeasonRequest request, User currentUser) {
        Season season = fetchSeason(seasonId, animeId, currentUser.getId());

        if (request.seasonNumber() != null && !request.seasonNumber().equals(season.getSeasonNumber())) {
            if (seasonRepository.existsByAnimeIdAndSeasonNumber(animeId, request.seasonNumber())) {
                throw new DuplicateResourceException("Season " + request.seasonNumber() + " already exists for this anime");
            }
            season.setSeasonNumber(request.seasonNumber());
        }

        if (request.title() != null) {
            season.setTitle(request.title());
        }
        if (request.status() != null) {
            season.setStatus(request.status());
        }
        if (request.totalEpisodes() != null) {
            season.setTotalEpisodes(request.totalEpisodes());
        }
        if (request.episodesWatched() != null) {
            season.setEpisodesWatched(request.episodesWatched());
        }
        if (request.rating() != null) {
            season.setRating(request.rating());
        }

        validateEpisodeCounts(season.getEpisodesWatched(), season.getTotalEpisodes());

        season.getAnime().touch();
        return SeasonResponse.from(seasonRepository.save(season));
    }

    @Transactional
    public void deleteSeason(Long animeId, Long seasonId, User currentUser) {
        Season season = fetchSeason(seasonId, animeId, currentUser.getId());
        season.getAnime().touch();
        seasonRepository.delete(season);
    }

    private Season fetchSeason(Long seasonId, Long animeId, Long userId) {
        return seasonRepository.findByIdAndAnimeIdAndAnimeUserId(seasonId, animeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id: " + seasonId));
    }

    public static void validateEpisodeCounts(Integer watched, Integer total) {
        if (total != null && watched != null && watched > total) {
            throw new BadRequestException("Episodes watched (" + watched + ") cannot exceed total episodes (" + total + ")");
        }
    }
}
