package com.repository;

import com.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByIdAndAnimeIdAndAnimeUserId(Long id, Long animeId, Long userId);

    boolean existsByAnimeIdAndSeasonNumber(Long animeId, Integer seasonNumber);

    @Query("SELECT COUNT(s) FROM Season s WHERE s.anime.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(s.episodesWatched), 0) FROM Season s WHERE s.anime.user.id = :userId")
    long sumEpisodesWatchedByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(s.rating) FROM Season s WHERE s.anime.user.id = :userId AND s.rating IS NOT NULL")
    Double findAverageRatingByUserId(@Param("userId") Long userId);

    @Query("SELECT s.status, COUNT(s) FROM Season s WHERE s.anime.user.id = :userId GROUP BY s.status")
    List<Object[]> countSeasonsByStatusForUser(@Param("userId") Long userId);
}
