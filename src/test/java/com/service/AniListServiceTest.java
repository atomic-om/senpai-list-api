package com.service;

import com.dto.anime.AnimeResponse;
import com.dto.discover.DiscoverAnimeResponse;
import com.entity.Season;
import com.entity.WatchStatus;
import com.exception.BadRequestException;
import com.exception.UpstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AniListServiceTest {

    private MockRestServiceServer server;
    private AniListService aniListService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        aniListService = new AniListService(builder, "https://graphql.anilist.co");
    }

    @Test
    void searchAnimePostsGraphQlAndMapsResponse() {
        String payload = """
                {
                  "data": {
                    "Page": {
                      "media": [
                        {
                          "id": 154587,
                          "title": {
                            "english": "Frieren: Beyond Journey's End",
                            "romaji": "Sousou no Frieren"
                          },
                          "coverImage": {
                            "extraLarge": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-extra.jpg",
                            "large": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx154587-large.jpg"
                          },
                          "episodes": 28,
                          "averageScore": 91,
                          "status": "FINISHED",
                          "description": "An elf mage embarks on a journey.<br><br><i>Award winner.</i>"
                        }
                      ]
                    }
                  }
                }
                """;

        server.expect(requestTo("https://graphql.anilist.co"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.query").value(containsString("POPULARITY_DESC")))
                .andExpect(jsonPath("$.variables.search").value("frieren"))
                .andExpect(jsonPath("$.variables.page").value(2))
                .andExpect(jsonPath("$.variables.perPage").value(12))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        List<DiscoverAnimeResponse> results = aniListService.searchAnime(" frieren ", 2);

        server.verify();
        assertEquals(1, results.size());
        DiscoverAnimeResponse item = results.get(0);
        assertEquals(154587L, item.anilistId());
        assertEquals("Frieren: Beyond Journey's End", item.title());
        assertEquals("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-extra.jpg", item.coverImageUrl());
        assertEquals(28, item.episodes());
        assertEquals(9.1, item.score());
        assertEquals("FINISHED", item.airingStatus());
        assertEquals("An elf mage embarks on a journey.Award winner.", item.synopsis());
    }

    @Test
    void getTopAnimeFallsBackToRomajiLargeCoverAndNullScore() {
        String payload = """
                {
                  "data": {
                    "Page": {
                      "media": [
                        {
                          "id": 5114,
                          "title": {
                            "english": "   ",
                            "romaji": "Hagane no Renkinjutsushi"
                          },
                          "coverImage": {
                            "extraLarge": null,
                            "large": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx5114-large.jpg"
                          },
                          "episodes": null,
                          "averageScore": null,
                          "status": "RELEASING",
                          "description": null
                        }
                      ]
                    }
                  }
                }
                """;

        server.expect(requestTo("https://graphql.anilist.co"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.query").value(containsString("TRENDING_DESC")))
                .andExpect(jsonPath("$.variables.page").value(1))
                .andExpect(jsonPath("$.variables.perPage").value(12))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        List<DiscoverAnimeResponse> results = aniListService.getTopAnime(1);

        server.verify();
        assertEquals(1, results.size());
        DiscoverAnimeResponse item = results.get(0);
        assertEquals(5114L, item.anilistId());
        assertEquals("Hagane no Renkinjutsushi", item.title());
        assertEquals("https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx5114-large.jpg", item.coverImageUrl());
        assertNull(item.episodes());
        assertNull(item.score());
        assertNull(item.synopsis());
    }

    @Test
    void rejectsBlankQueryAndInvalidPage() {
        assertThrows(BadRequestException.class, () -> aniListService.searchAnime("   ", 1));
        assertThrows(BadRequestException.class, () -> aniListService.searchAnime("naruto", 0));
        assertThrows(BadRequestException.class, () -> aniListService.getTopAnime(0));
    }

    @Test
    void rateLimitNetworkAndServerErrorsMapToUpstreamServiceException() {
        server.expect(requestTo("https://graphql.anilist.co"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        UpstreamServiceException rateLimitEx = assertThrows(
                UpstreamServiceException.class,
                () -> aniListService.getTopAnime(1)
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, rateLimitEx.getStatus());

        server.reset();
        server.expect(requestTo("https://graphql.anilist.co"))
                .andRespond(withServerError());

        UpstreamServiceException serverEx = assertThrows(
                UpstreamServiceException.class,
                () -> aniListService.getTopAnime(1)
        );
        assertEquals(HttpStatus.BAD_GATEWAY, serverEx.getStatus());

        server.reset();
        server.expect(requestTo("https://graphql.anilist.co"))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out");
                });

        UpstreamServiceException timeoutEx = assertThrows(
                UpstreamServiceException.class,
                () -> aniListService.getTopAnime(1)
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, timeoutEx.getStatus());
    }

    @Test
    void deriveOverallStatusFollowsAllRules() {
        assertEquals(WatchStatus.PLAN_TO_WATCH, AnimeResponse.deriveOverallStatus(List.of()));
        assertEquals(WatchStatus.PLAN_TO_WATCH, AnimeResponse.deriveOverallStatus(List.of(
                season(WatchStatus.PLAN_TO_WATCH),
                season(WatchStatus.PLAN_TO_WATCH)
        )));
        assertEquals(WatchStatus.WATCHING, AnimeResponse.deriveOverallStatus(List.of(
                season(WatchStatus.COMPLETED),
                season(WatchStatus.WATCHING),
                season(WatchStatus.ON_HOLD)
        )));
        assertEquals(WatchStatus.WATCHING, AnimeResponse.deriveOverallStatus(List.of(
                season(WatchStatus.COMPLETED),
                season(WatchStatus.PLAN_TO_WATCH)
        )));
        assertEquals(WatchStatus.ON_HOLD, AnimeResponse.deriveOverallStatus(List.of(
                season(WatchStatus.COMPLETED),
                season(WatchStatus.ON_HOLD)
        )));
        assertEquals(WatchStatus.COMPLETED, AnimeResponse.deriveOverallStatus(List.of(
                season(WatchStatus.COMPLETED),
                season(WatchStatus.COMPLETED)
        )));
        assertEquals(WatchStatus.DROPPED, AnimeResponse.deriveOverallStatus(List.of(
                season(WatchStatus.DROPPED),
                season(WatchStatus.DROPPED)
        )));
    }

    private Season season(WatchStatus status) {
        return Season.builder()
                .seasonNumber(1)
                .status(status)
                .build();
    }
}
