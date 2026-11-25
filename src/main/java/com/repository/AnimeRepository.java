package com.repository;

import com.entity.Anime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AnimeRepository extends JpaRepository<Anime, Long>, JpaSpecificationExecutor<Anime> {
    @EntityGraph(attributePaths = "seasons")
    Optional<Anime> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTitleIgnoreCase(Long userId, String title);

    long countByUserId(Long userId);
}
