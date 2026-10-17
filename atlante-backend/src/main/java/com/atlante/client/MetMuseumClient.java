package com.atlante.client;

import com.atlante.dto.Dtos.ExternalSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class MetMuseumClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetMuseumClient.class);
    private static final int MAX_RESULTS = 10;

    private final WebClient webClient;

    public MetMuseumClient(@Qualifier("metWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /** Se il Met non risponde (es. 410 Gone) la ricerca è vuota: le altre fonti restano utilizzabili. */
    public List<ExternalSearchResult> search(String query) {
        JsonNode root;
        try {
            root = webClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", query)
                            .queryParam("hasImages", "true")
                            .build())
                    .retrieve().bodyToMono(JsonNode.class).block();
        } catch (Exception e) {
            log.warn("ricerca Met non disponibile ({}): si procede senza", e.getMessage());
            return List.of();
        }

        List<ExternalSearchResult> results = new ArrayList<>();
        if (root != null && root.has("objectIDs") && !root.get("objectIDs").isNull()) {
            int count = 0;
            for (JsonNode idNode : root.get("objectIDs")) {
                if (count++ >= MAX_RESULTS) {
                    break;
                }
                findById(idNode.asText()).ifPresent(results::add);
            }
        }
        return results;
    }

    public Optional<ExternalSearchResult> findById(String objectId) {
        JsonNode n = webClient.get()
                .uri("/objects/" + objectId)
                .retrieve().bodyToMono(JsonNode.class)
                .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                .block();
        if (n == null || n.path("objectID").isMissingNode() || n.path("objectID").asLong() == 0) {
            return Optional.empty();
        }
        String image = n.path("primaryImageSmall").asText(null);
        if (image == null || image.isBlank()) {
            image = n.path("primaryImage").asText(null);
        }
        return Optional.of(new ExternalSearchResult(
                n.path("objectID").asText(),
                n.path("title").asText(),
                n.path("artistDisplayName").asText(null),
                parseYear(n.path("objectEndDate").asText(null)),
                image,
                "MET"));
    }

    private Integer parseYear(String value) {
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
