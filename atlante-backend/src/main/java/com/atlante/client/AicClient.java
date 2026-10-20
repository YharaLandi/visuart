package com.atlante.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

/**
 * Art Institute of Chicago — API pubblica senza chiave.
 * Usata come fonte immagini per opere non presenti al Met.
 */
@Component
public class AicClient {

    private final WebClient webClient =
            WebClient.builder().baseUrl("https://api.artic.edu/api/v1").build();

    public java.util.List<com.atlante.dto.Dtos.ExternalSearchResult> search(String query) {
        java.util.List<com.atlante.dto.Dtos.ExternalSearchResult> results = new java.util.ArrayList<>();
        try {
            JsonNode root = webClient.get()
                    .uri(uri -> uri.path("/artworks/search")
                            .queryParam("q", query)
                            .queryParam("fields", "id,title,artist_display,image_id,date_end")
                            .queryParam("limit", "12")
                            .build())
                    .retrieve().bodyToMono(JsonNode.class).block();
            if (root == null || !root.has("data")) {
                return results;
            }
            for (JsonNode n : root.get("data")) {
                String artist = n.path("artist_display").asText(null);
                if (artist != null && artist.contains("\n")) {
                    artist = artist.substring(0, artist.indexOf('\n'));
                }
                results.add(new com.atlante.dto.Dtos.ExternalSearchResult(
                        n.path("id").asText(),
                        n.path("title").asText(),
                        artist,
                        n.hasNonNull("date_end") ? n.get("date_end").asInt() : null,
                        null, // immagine IIIF bloccata fuori da artic.edu: la scheda mostra il segnaposto
                        "AIC"));
            }
        } catch (Exception e) {
            // AIC non raggiungibile: si prosegue col fallback Met
        }
        return results;
    }

    public Optional<String> findImageUrl(String title, String artist) {
        try {
            String query = artist != null ? title + " " + artist : title;
            JsonNode root = webClient.get()
                    .uri(uri -> uri.path("/artworks/search")
                            .queryParam("q", query)
                            .queryParam("fields", "id,title,image_id")
                            .queryParam("limit", "5")
                            .build())
                    .retrieve().bodyToMono(JsonNode.class).block();
            if (root == null || !root.has("data")) {
                return Optional.empty();
            }
            for (JsonNode n : root.get("data")) {
                if (n.hasNonNull("image_id")) {
                    return Optional.of("https://www.artic.edu/iiif/2/"
                            + n.get("image_id").asText() + "/full/600,/0/default.jpg");
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
