package com.service;

import com.dto.anime.AnimeResponse;
import com.dto.anime.CreateAnimeRequest;
import com.dto.anime.UpdateAnimeRequest;
import com.dto.season.SeasonRequest;
import com.entity.Anime;
import com.entity.Season;
import com.entity.User;
import com.entity.WatchStatus;
import com.exception.ResourceNotFoundException;
import com.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnimeService {

    private final AnimeRepository animeRepository;

    @Transactional
    public AnimeResponse addAnime(CreateAnimeRequest request, User currentUser) {
        String trimmedTitle = request.title().trim();

        if (animeRepository.existsByUserIdAndTitleIgnoreCase(currentUser.getId(), trimmedTitle)) {
            throw new IllegalArgumentException("Anime '" + trimmedTitle + "' is already in your watchlist");
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
                    throw new IllegalArgumentException("Duplicate season number: " + seasonReq.seasonNumber());
                }
                int watched = seasonReq.episodesWatched() != null ? seasonReq.episodesWatched() : 0;
                if (seasonReq.totalEpisodes() != null && watched > seasonReq.totalEpisodes()) {
                    throw new IllegalArgumentException(
                            "Episodes watched (" + watched + ") cannot exceed total episodes (" + seasonReq.totalEpisodes() + ")"
                    );
                }

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
    public List<AnimeResponse> allAnime(User currentUser) {
        return animeRepository.findAllByUserIdOrderByUpdatedAtDesc(currentUser.getId())
                .stream()
                .map(AnimeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnimeResponse findAnime(Long animeId, User currentUser) {
        return AnimeResponse.from(fetchAnime(animeId, currentUser.getId()));
    }

    @Transactional
    public AnimeResponse updateAnime(Long animeId, UpdateAnimeRequest request, User currentUser) {
        Anime anime = fetchAnime(animeId, currentUser.getId());

        if (request.title() != null && !request.title().isBlank()) {
            String trimmedTitle = request.title().trim();

            if (!anime.getTitle().equalsIgnoreCase(trimmedTitle)
                    && animeRepository.existsByUserIdAndTitleIgnoreCase(currentUser.getId(), trimmedTitle)) {
                throw new IllegalArgumentException("Anime '" + trimmedTitle + "' is already in your watchlist");
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

    public Anime fetchAnime(Long animeId, Long userId) {
        return animeRepository.findByIdAndUserId(animeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found with id: " + animeId));
    }
}
