package com;

import com.dto.discover.DiscoverAnimeResponse;
import com.exception.UpstreamServiceException;
import com.repository.AnimeRepository;
import com.repository.UserRepository;
import com.service.AniListService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SenpaiListIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @MockitoBean
    private AniListService aniListService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        animeRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerUser(String username, String email, String password) throws Exception {
        String body = """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(username, email, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        return extractJsonString(result.getResponse().getContentAsString(), "token");
    }

    private long createAnime(String token, String jsonBody) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andReturn();

        return extractJsonLong(result.getResponse().getContentAsString(), "id");
    }

    private String extractJsonString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Key '" + key + "' not found in: " + json);
        }
        return matcher.group(1);
    }

    private long extractJsonLong(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Numeric key '" + key + "' not found in: " + json);
        }
        return Long.parseLong(matcher.group(1));
    }

    @Test
    void authEndpointsAndErrorHandlingWork() throws Exception {
        String token = registerUser("naruto", "naruto@senpailist.com", "secret123");

        // Duplicate email -> 409
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"other","email":"naruto@senpailist.com","password":"secret123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Duplicate username -> 409
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"naruto","email":"other@senpailist.com","password":"secret123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Valid login -> 200
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"naruto@senpailist.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // Bad credentials -> 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"naruto@senpailist.com","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Missing token -> 401 ErrorResponse (not 403)
        mockMvc.perform(get("/api/v1/animes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Empty Bearer token -> 401 ErrorResponse
        mockMvc.perform(get("/api/v1/animes")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Malformed JWT -> 401 ErrorResponse
        mockMvc.perform(get("/api/v1/animes")
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid JWT token"));

        // Expired JWT -> 401 ErrorResponse
        String expiredToken = Jwts.builder()
                .subject("naruto@senpailist.com")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();

        mockMvc.perform(get("/api/v1/animes")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("JWT token has expired"));

        // Expired JWT sent to /api/v1/auth/login is ignored by shouldNotFilter -> 200
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"naruto@senpailist.com","password":"secret123"}
                                """))
                .andExpect(status().isOk());

        // Valid token works
        mockMvc.perform(get("/api/v1/animes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void validationAndConflictHandlersWork() throws Exception {
        String token = registerUser("sasuke", "sasuke@senpailist.com", "secret123");

        // Blank anime title -> 400
        mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.title").exists());

        // Malformed JSON -> 400
        mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // episodesWatched > totalEpisodes -> 400
        mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bleach",
                                  "seasons": [
                                    {
                                      "seasonNumber": 1,
                                      "episodesWatched": 25,
                                      "totalEpisodes": 24
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        long animeId = createAnime(token, """
                {
                  "title": "Bleach",
                  "seasons": [{"seasonNumber": 1, "status": "WATCHING", "episodesWatched": 5, "totalEpisodes": 24}]
                }
                """);

        // Blank title on PATCH -> 400
        mockMvc.perform(patch("/api/v1/animes/" + animeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Duplicate anime title (case-insensitive) -> 409
        mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"bleach"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Duplicate season number -> 409
        mockMvc.perform(post("/api/v1/animes/" + animeId + "/seasons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seasonNumber": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Invalid sort property -> 400
        mockMvc.perform(get("/api/v1/animes?sort=nonExistentField")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Invalid enum query param -> 400
        mockMvc.perform(get("/api/v1/animes?status=NOT_A_STATUS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void userIsolationReturns404() throws Exception {
        String userAToken = registerUser("userA", "a@senpailist.com", "secret123");
        String userBToken = registerUser("userB", "b@senpailist.com", "secret123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Frieren",
                                  "seasons": [{"seasonNumber": 1, "status": "COMPLETED", "episodesWatched": 28, "totalEpisodes": 28, "rating": 10}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        long animeId = extractJsonLong(body, "id");
        String seasonsPart = body.substring(body.indexOf("\"seasons\""));
        long seasonId = extractJsonLong(seasonsPart, "id");

        mockMvc.perform(get("/api/v1/animes/" + animeId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/animes/" + animeId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hacked"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/animes/" + animeId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/animes/" + animeId + "/seasons")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/animes/" + animeId + "/seasons/" + seasonId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 1}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/animes/" + animeId + "/seasons/" + seasonId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/animes")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void searchPaginationAndStatsWork() throws Exception {
        String token = registerUser("gojo", "gojo@senpailist.com", "secret123");

        long jjkId = createAnime(token, """
                {
                  "title": "Jujutsu Kaisen",
                  "seasons": [
                    {"seasonNumber": 2, "status": "WATCHING", "episodesWatched": 10, "totalEpisodes": 23, "rating": 9},
                    {"seasonNumber": 1, "status": "COMPLETED", "episodesWatched": 24, "totalEpisodes": 24, "rating": 8}
                  ]
                }
                """);

        createAnime(token, """
                {
                  "title": "Attack on Titan",
                  "seasons": [
                    {"seasonNumber": 1, "status": "COMPLETED", "episodesWatched": 25, "totalEpisodes": 25, "rating": 10}
                  ]
                }
                """);

        createAnime(token, """
                {
                  "title": "Chainsaw Man",
                  "seasons": [
                    {"seasonNumber": 1, "status": "PLAN_TO_WATCH", "episodesWatched": 0, "totalEpisodes": 12}
                  ]
                }
                """);

        // Seasons returned sorted by seasonNumber ASC
        mockMvc.perform(get("/api/v1/animes/" + jjkId + "/seasons")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].seasonNumber").value(1))
                .andExpect(jsonPath("$[1].seasonNumber").value(2));

        // Search filter
        mockMvc.perform(get("/api/v1/animes?search=kaisen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Jujutsu Kaisen"));

        // Status filter
        mockMvc.perform(get("/api/v1/animes?status=COMPLETED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        // Pagination
        mockMvc.perform(get("/api/v1/animes?page=0&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        // Stats
        mockMvc.perform(get("/api/v1/animes/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAnimes").value(3))
                .andExpect(jsonPath("$.totalSeasons").value(4))
                .andExpect(jsonPath("$.totalEpisodesWatched").value(59))
                .andExpect(jsonPath("$.averageRating").value(9.0))
                .andExpect(jsonPath("$.seasonsByStatus.COMPLETED").value(2))
                .andExpect(jsonPath("$.seasonsByStatus.WATCHING").value(1))
                .andExpect(jsonPath("$.seasonsByStatus.PLAN_TO_WATCH").value(1))
                .andExpect(jsonPath("$.seasonsByStatus.ON_HOLD").value(0))
                .andExpect(jsonPath("$.seasonsByStatus.DROPPED").value(0));
    }

    @Test
    void openApiEndpointResponds() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SenpaiList API"));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void animeResponseIncludesDerivedOverallStatus() throws Exception {
        String token = registerUser("luffy", "luffy@senpailist.com", "secret123");

        // Empty seasons -> PLAN_TO_WATCH
        mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "One Piece"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overallStatus").value("PLAN_TO_WATCH"));

        // Mixed COMPLETED + PLAN_TO_WATCH -> WATCHING
        mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Vinland Saga",
                                  "seasons": [
                                    {"seasonNumber": 1, "status": "COMPLETED", "episodesWatched": 24, "totalEpisodes": 24},
                                    {"seasonNumber": 2, "status": "PLAN_TO_WATCH"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overallStatus").value("WATCHING"));

        // COMPLETED + ON_HOLD (no WATCHING) -> ON_HOLD
        mockMvc.perform(post("/api/v1/animes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Mob Psycho 100",
                                  "seasons": [
                                    {"seasonNumber": 1, "status": "COMPLETED"},
                                    {"seasonNumber": 2, "status": "ON_HOLD"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overallStatus").value("ON_HOLD"));
    }

    @Test
    void discoverEndpointsAndUpstreamErrorsWork() throws Exception {
        String token = registerUser("zoro", "zoro@senpailist.com", "secret123");

        // Unauthenticated -> 401
        mockMvc.perform(get("/api/v1/discover/top"))
                .andExpect(status().isUnauthorized());

        // Blank q -> 400
        mockMvc.perform(get("/api/v1/discover/search?q=   ")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Page < 1 -> 400
        mockMvc.perform(get("/api/v1/discover/top?page=0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Search -> 200
        when(aniListService.searchAnime("frieren", 1)).thenReturn(List.of(
                new DiscoverAnimeResponse(
                        154587L,
                        "Frieren: Beyond Journey's End",
                        "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-extra.jpg",
                        28,
                        9.1,
                        "FINISHED",
                        "An elf mage embarks on a journey."
                )
        ));

        mockMvc.perform(get("/api/v1/discover/search?q=frieren")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].anilistId").value(154587))
                .andExpect(jsonPath("$[0].title").value("Frieren: Beyond Journey's End"))
                .andExpect(jsonPath("$[0].coverImageUrl").value("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-extra.jpg"))
                .andExpect(jsonPath("$[0].episodes").value(28))
                .andExpect(jsonPath("$[0].score").value(9.1))
                .andExpect(jsonPath("$[0].airingStatus").value("FINISHED"));

        // Top -> 200
        when(aniListService.getTopAnime(2)).thenReturn(List.of(
                new DiscoverAnimeResponse(
                        5114L,
                        "Fullmetal Alchemist: Brotherhood",
                        "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx5114-extra.jpg",
                        64,
                        9.0,
                        "FINISHED",
                        "Two brothers search for the Philosopher's Stone."
                )
        ));

        mockMvc.perform(get("/api/v1/discover/top?page=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].anilistId").value(5114));

        // Upstream rate limit -> 503 ErrorResponse
        when(aniListService.getTopAnime(3)).thenThrow(
                new UpstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AniList API rate limit exceeded. Please try again later.")
        );

        mockMvc.perform(get("/api/v1/discover/top?page=3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"));

        // Upstream error -> 502 ErrorResponse
        when(aniListService.searchAnime("error", 1)).thenThrow(
                new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Upstream AniList API error (status 500)")
        );

        mockMvc.perform(get("/api/v1/discover/search?q=error")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"));
    }

    @Test
    void corsPreflightAllowsConfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/v1/animes")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")));

        mockMvc.perform(options("/api/v1/discover/top")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mockMvc.perform(options("/api/v1/animes")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
