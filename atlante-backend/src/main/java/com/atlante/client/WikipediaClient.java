package com.atlante.client;

import com.atlante.dto.Dtos.ExternalSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ricerca opere d'arte su Wikipedia in italiano: copertura universale
 * (qualunque museo) e ricerca in lingua italiana. Si filtrano le voci la cui
 * descrizione indica un'opera pittorica.
 */
@Component
public class WikipediaClient {

    private static final Pattern ART_DESCRIPTION = Pattern.compile(
            "dipint|quadro|tela|affresc|olio su|pittur|painting", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATOR = Pattern.compile(
            "(?:dipinto|quadro|tela|affresco|opera)\\s+(?:a olio\\s+)?di\\s+([A-ZÀ-Ý][^,;.()]+)");
    private static final Pattern YEAR = Pattern.compile("(1[0-9]{3}|20[0-2][0-9])");

    private final WebClient webClient =
            WebClient.builder().baseUrl("https://it.wikipedia.org/w").build();

    public List<ExternalSearchResult> search(String query) {
        List<ExternalSearchResult> results = new ArrayList<>();
        try {
            JsonNode root = webClient.get()
                    .uri(uri -> uri.path("/api.php")
                            .queryParam("action", "query")
                            .queryParam("format", "json")
                            .queryParam("generator", "search")
                            .queryParam("gsrsearch", query)
                            .queryParam("gsrlimit", "15")
                            .queryParam("prop", "pageimages|description")
                            .queryParam("piprop", "thumbnail")
                            .queryParam("pithumbsize", "600")
                            .build())
                    .header("User-Agent", "AtlanteDelleInfluenze/1.0 (progetto didattico Epicode)")
                    .retrieve().bodyToMono(JsonNode.class).block();

            JsonNode pages = root != null ? root.path("query").path("pages") : null;
            if (pages == null || pages.isMissingNode()) {
                return results;
            }
            List<JsonNode> sorted = new ArrayList<>();
            pages.forEach(sorted::add);
            sorted.sort(Comparator.comparingInt(p -> p.path("index").asInt(99)));

            for (JsonNode page : sorted) {
                String description = page.path("description").asText("");
                if (!ART_DESCRIPTION.matcher(description).find()) {
                    continue;
                }
                String thumb = page.path("thumbnail").path("source").asText(null);
                if (thumb == null) {
                    continue;
                }
                results.add(new ExternalSearchResult(
                        page.path("pageid").asText(),
                        page.path("title").asText(),
                        extract(CREATOR, description),
                        extractYear(description),
                        thumb,
                        "WIKIPEDIA"));
            }
        } catch (Exception e) {
            // Wikipedia non raggiungibile: si prosegue coi fallback museali
        }
        return results;
    }

    private String extract(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private Integer extractYear(String text) {
        Matcher m = YEAR.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
