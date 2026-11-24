package com.repository;

import com.entity.Anime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnimeRepository extends JpaRepository<Anime, Long> {
    @EntityGraph(attributePaths = "seasons")
    List<Anime> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "seasons")
    Optional<Anime> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTitleIgnoreCase(Long userId, String title);
}
