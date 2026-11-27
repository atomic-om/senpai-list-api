package com.controller;

import com.dto.discover.DiscoverAnimeResponse;
import com.service.AniListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discover")
@RequiredArgsConstructor
public class DiscoverController {

    private final AniListService aniListService;

    @GetMapping("/search")
    public ResponseEntity<List<DiscoverAnimeResponse>> searchAnime(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(aniListService.searchAnime(q, page));
    }

    @GetMapping("/top")
    public ResponseEntity<List<DiscoverAnimeResponse>> topAnime(
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(aniListService.getTopAnime(page));
    }
}
