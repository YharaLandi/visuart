package com.atlante.client;

import com.atlante.dto.Dtos.ExternalSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TmdbClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String imageBaseUrl;

    public TmdbClient(@Qualifier("tmdbWebClient") WebClient webClient,
                      @Value("${app.tmdb.api-key}") String apiKey,
                      @Value("${app.tmdb.image-base-url}") String imageBaseUrl) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.imageBaseUrl = imageBaseUrl;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<ExternalSearchResult> search(String query) {
        requireKey();
        JsonNode root = webClient.get()
                .uri(uri -> uri.path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", query)
                        .queryParam("language", "it-IT")
                        .build())
                .retrieve().bodyToMono(JsonNode.class).block();

        List<ExternalSearchResult> results = new ArrayList<>();
        if (root != null && root.has("results")) {
            for (JsonNode n : root.get("results")) {
                results.add(new ExternalSearchResult(
                        n.path("id").asText(),
                        n.path("title").asText(),
                        null,
                        parseYear(n.path("release_date").asText(null)),
                        n.hasNonNull("poster_path") ? imageBaseUrl + n.get("poster_path").asText() : null,
                        "TMDB"));
            }
        }
        return results;
    }

    public Optional<ExternalSearchResult> findById(String tmdbId) {
        requireKey();
        JsonNode n = webClient.get()
                .uri(uri -> uri.path("/movie/" + tmdbId)
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "it-IT")
                        .build())
                .retrieve().bodyToMono(JsonNode.class).block();
        if (n == null || n.has("status_code")) {
            return Optional.empty();
        }
        return Optional.of(new ExternalSearchResult(
                n.path("id").asText(),
                n.path("title").asText(),
                null,
                parseYear(n.path("release_date").asText(null)),
                n.hasNonNull("poster_path") ? imageBaseUrl + n.get("poster_path").asText() : null,
                "TMDB"));
    }

    private void requireKey() {
        if (!isConfigured()) {
            throw new IllegalArgumentException(
                    "TMDB API key non configurata: imposta la variabile d'ambiente TMDB_API_KEY");
        }
    }

    private Integer parseYear(String date) {
        if (date == null || date.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
