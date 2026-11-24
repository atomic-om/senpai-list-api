package com.controller;

import com.dto.season.SeasonRequest;
import com.dto.season.SeasonResponse;
import com.dto.season.UpdateSeasonRequest;
import com.entity.User;
import com.service.SeasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/animes/{animeId}/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;

    @GetMapping
    public ResponseEntity<List<SeasonResponse>> allSeason(
            @PathVariable Long animeId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(seasonService.allSeason(animeId, currentUser));
    }

    @GetMapping("/{seasonId}")
    public ResponseEntity<SeasonResponse> findSeason(
            @PathVariable Long animeId,
            @PathVariable Long seasonId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(seasonService.findSeason(animeId, seasonId, currentUser));
    }

    @PostMapping
    public ResponseEntity<SeasonResponse> addSeason(
            @PathVariable Long animeId,
            @Valid @RequestBody SeasonRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seasonService.addSeason(animeId, request, currentUser));
    }

    @PatchMapping("/{seasonId}")
    public ResponseEntity<SeasonResponse> updateSeason(
            @PathVariable Long animeId,
            @PathVariable Long seasonId,
            @Valid @RequestBody UpdateSeasonRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(seasonService.updateSeason(animeId, seasonId, request, currentUser));
    }

    @DeleteMapping("/{seasonId}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable Long animeId,
            @PathVariable Long seasonId,
            @AuthenticationPrincipal User currentUser
    ) {
        seasonService.deleteSeason(animeId, seasonId, currentUser);
        return ResponseEntity.noContent().build();
    }
}