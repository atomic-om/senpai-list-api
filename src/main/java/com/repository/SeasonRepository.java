package com.repository;

import com.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByIdAndAnimeIdAndAnimeUserId(Long id, Long animeId, Long userId);

    boolean existsByAnimeIdAndSeasonNumber(Long animeId, Integer seasonNumber);

    List<Season> findAllByAnimeIdAndAnimeUserIdOrderBySeasonNumberAsc(Long animeId, Long userId);
}
