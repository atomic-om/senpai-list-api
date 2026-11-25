package com.repository;

import com.entity.Anime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AnimeRepository extends JpaRepository<Anime, Long>, JpaSpecificationExecutor<Anime> {
    @Override
    @NullMarked
    @EntityGraph(attributePaths = {"seasons"})
    Page<Anime> findAll(@Nullable Specification<Anime> spec, Pageable pageable);

    @EntityGraph(attributePaths = "seasons")
    Optional<Anime> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTitleIgnoreCase(Long userId, String title);

    long countByUserId(Long userId);
}
