package com.controller;

import com.dto.anime.AnimeResponse;
import com.dto.anime.CreateAnimeRequest;
import com.dto.anime.UpdateAnimeRequest;
import com.entity.User;
import com.service.AnimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<AnimeResponse>> allAnime(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(animeService.allAnime(currentUser));
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
}
