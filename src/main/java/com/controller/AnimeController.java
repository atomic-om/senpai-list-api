package com.controller;

import com.dto.StatsResponse;
import com.dto.anime.AnimeResponse;
import com.dto.anime.CreateAnimeRequest;
import com.dto.anime.UpdateAnimeRequest;
import com.entity.User;
import com.entity.WatchStatus;
import com.service.AnimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/animes")
@RequiredArgsConstructor
public class AnimeController {

    private final AnimeService animeService;

    @PostMapping
    public ResponseEntity<AnimeResponse> addAnime(
            @Valid @RequestBody CreateAnimeRequest request,
            @AuthenticationPrincipal User currentUser
            ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(animeService.addAnime(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<Page<AnimeResponse>> allAnime(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WatchStatus status,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(animeService.allAnime(currentUser, search, status, pageable));
    }

    @GetMapping("/{animeId}")
    public ResponseEntity<AnimeResponse> findAnime(
            @PathVariable Long animeId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(animeService.findAnime(animeId, currentUser));
    }

    @PatchMapping("/{animeId}")
    public ResponseEntity<AnimeResponse> updateAnime(
            @PathVariable Long animeId,
            @RequestBody UpdateAnimeRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(animeService.updateAnime(animeId, request, currentUser));
    }

    @DeleteMapping("/{animeId}")
    public ResponseEntity<Void> deleteAnime(
            @PathVariable Long animeId,
            @AuthenticationPrincipal User currentUse
    ) {
        animeService.deleteAnime(animeId, currentUse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(animeService.getStats(currentUser));
    }
}
