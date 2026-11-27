package com.service;

import com.dto.discover.DiscoverAnimeResponse;
import com.exception.BadRequestException;
import com.exception.UpstreamServiceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AniListService {

    private static final int DEFAULT_PER_PAGE = 12;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final String SEARCH_QUERY = """
            query ($search: String, $page: Int, $perPage: Int) {
              Page(page: $page, perPage: $perPage) {
                media(search: $search, type: ANIME, isAdult: false, sort: POPULARITY_DESC) {
                  id
                  title {
                    english
                    romaji
                  }
                  coverImage {
                    extraLarge
                    large
                  }
                  episodes
                  averageScore
                  status
                  description
                }
              }
            }
            """;

    private static final String TOP_QUERY = """
            query ($page: Int, $perPage: Int) {
              Page(page: $page, perPage: $perPage) {
                media(type: ANIME, isAdult: false, sort: TRENDING_DESC) {
                  id
                  title {
                    english
                    romaji
                  }
                  coverImage {
                    extraLarge
                    large
                  }
                  episodes
                  averageScore
                  status
                  description
                }
              }
            }
            """;

    private final RestClient restClient;

    @Autowired
    public AniListService(
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            @Value("${anilist.api.url:https://graphql.anilist.co}") String apiUrl
    ) {
        this(configureProductionBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder)), apiUrl);
    }

    AniListService(RestClient.Builder builder, String apiUrl) {
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static RestClient.Builder configureProductionBuilder(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        return builder.requestFactory(requestFactory);
    }

    public List<DiscoverAnimeResponse> searchAnime(String query, int page) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search query 'q' must not be blank");
        }
        validatePage(page);

        GraphQlRequest request = new GraphQlRequest(
                SEARCH_QUERY,
                Map.of(
                        "search", query.trim(),
                        "page", page,
                        "perPage", DEFAULT_PER_PAGE
                )
        );
        return executeQuery(request);
    }

    public List<DiscoverAnimeResponse> getTopAnime(int page) {
        validatePage(page);

        GraphQlRequest request = new GraphQlRequest(
                TOP_QUERY,
                Map.of(
                        "page", page,
                        "perPage", DEFAULT_PER_PAGE
                )
        );
        return executeQuery(request);
    }

    private void validatePage(int page) {
        if (page < 1) {
            throw new BadRequestException("Page must be at least 1");
        }
    }

    private List<DiscoverAnimeResponse> executeQuery(GraphQlRequest request) {
        try {
            AniListResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AniListResponse.class);

            if (response == null) {
                return List.of();
            }
            if (response.errors() != null && !response.errors().isEmpty() && response.data() == null) {
                throw new UpstreamServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "Upstream AniList API returned an error"
                );
            }
            if (response.data() == null
                    || response.data().page() == null
                    || response.data().page().media() == null) {
                return List.of();
            }
            return response.data().page().media().stream()
                    .map(this::toDiscoverResponse)
                    .toList();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw new UpstreamServiceException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AniList API rate limit exceeded. Please try again later."
                );
            }
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Upstream AniList API error (status " + ex.getStatusCode().value() + ")"
            );
        } catch (ResourceAccessException ex) {
            throw new UpstreamServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to reach AniList API. Please try again later."
            );
        }
    }

    private DiscoverAnimeResponse toDiscoverResponse(AniListMedia media) {
        String title = resolveTitle(media.title());
        String coverImageUrl = resolveCoverImage(media.coverImage());
        Double score = media.averageScore() != null
                ? media.averageScore() / 10.0
                : null;
        String synopsis = sanitizeSynopsis(media.description());

        return new DiscoverAnimeResponse(
                media.id(),
                title,
                coverImageUrl,
                media.episodes(),
                score,
                media.status(),
                synopsis
        );
    }

    private String sanitizeSynopsis(String description) {
        if (description == null) {
            return null;
        }
        return description
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]*>", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&amp;", "&")
                .trim();
    }

    private String resolveTitle(AniListTitle title) {
        if (title == null) {
            return null;
        }
        if (title.english() != null && !title.english().isBlank()) {
            return title.english();
        }
        return title.romaji();
    }

    private String resolveCoverImage(AniListCoverImage coverImage) {
        if (coverImage == null) {
            return null;
        }
        if (coverImage.extraLarge() != null && !coverImage.extraLarge().isBlank()) {
            return coverImage.extraLarge();
        }
        return coverImage.large();
    }

    private record GraphQlRequest(String query, Map<String, Object> variables) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListResponse(AniListData data, List<Map<String, Object>> errors) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListData(AniListPage Page) {
        AniListPage page() {
            return Page;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListPage(List<AniListMedia> media) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListMedia(
            Long id,
            AniListTitle title,
            AniListCoverImage coverImage,
            Integer episodes,
            Integer averageScore,
            String status,
            String description
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListTitle(String english, String romaji) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AniListCoverImage(String extraLarge, String large) {
    }
}
