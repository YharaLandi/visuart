package com.atlante.client;

import com.atlante.dto.Dtos.ExternalSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ricerca opere d'arte su Wikipedia, prima in italiano e poi in inglese (molte più voci).
 * È anche la fonte delle immagini dei quadri: le miniature di Wikimedia si caricano ovunque,
 * a differenza del server IIIF dell'Art Institute of Chicago che rifiuta le richieste esterne.
 */
@Component
public class WikipediaClient {

    private static final String USER_AGENT = "VisuArt/1.0 (progetto didattico Epicode)";

    private static final Pattern ART_DESCRIPTION = Pattern.compile(
            "dipint|quadro|tela|affresc|olio su|pittur|painting|artwork|fresco|portrait by|oil on",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATOR_IT = Pattern.compile(
            "(?:dipinto|quadro|tela|affresco|opera)\\s+(?:a olio\\s+)?di\\s+([A-ZÀ-Ý][^,;.()]+)");
    private static final Pattern CREATOR_EN = Pattern.compile("\\bby\\s+([A-ZÀ-Ý][^,;.()]+)");
    private static final Pattern YEAR = Pattern.compile("(1[0-9]{3}|20[0-2][0-9])");

    private final Map<String, WebClient> clients = new LinkedHashMap<>();

    public WikipediaClient() {
        for (String lang : List.of("it", "en")) {
            clients.put(lang, WebClient.builder().baseUrl("https://" + lang + ".wikipedia.org/w").build());
        }
    }

    /** Quadri che corrispondono alla ricerca: voci italiane prima, poi inglesi, senza doppioni. */
    public List<ExternalSearchResult> search(String query) {
        List<ExternalSearchResult> results = new ArrayList<>();
        for (String lang : clients.keySet()) {
            for (ExternalSearchResult r : search(lang, query, true)) {
                boolean duplicate = results.stream().anyMatch(x -> x.imageUrl().equals(r.imageUrl()));
                if (!duplicate) {
                    results.add(r);
                }
            }
            if (results.size() >= 8) {
                break;
            }
        }
        return results;
    }

    /** Immagine per un titolo (e autore): usata per opere e suggerimenti dell'AI. */
    public Optional<String> findImageUrl(String title, String creator) {
        String query = creator != null && !creator.isBlank() ? title + " " + creator : title;
        for (String lang : List.of("en", "it")) {
            List<ExternalSearchResult> found = search(lang, query, false);
            if (!found.isEmpty()) {
                return Optional.of(found.getFirst().imageUrl());
            }
        }
        return Optional.empty();
    }

    private List<ExternalSearchResult> search(String lang, String query, boolean onlyArtworks) {
        List<ExternalSearchResult> results = new ArrayList<>();
        try {
            JsonNode root = clients.get(lang).get()
                    .uri(uri -> uri.path("/api.php")
                            .queryParam("action", "query")
                            .queryParam("format", "json")
                            .queryParam("generator", "search")
                            .queryParam("gsrsearch", query)
                            .queryParam("gsrlimit", onlyArtworks ? "15" : "3")
                            .queryParam("prop", "pageimages|description")
                            .queryParam("piprop", "thumbnail")
                            .queryParam("pithumbsize", "800")
                            .build())
                    .header("User-Agent", USER_AGENT)
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
                if (onlyArtworks && !ART_DESCRIPTION.matcher(description).find()) {
                    continue;
                }
                String thumb = page.path("thumbnail").path("source").asText(null);
                if (thumb == null) {
                    continue;
                }
                results.add(new ExternalSearchResult(
                        lang + ":" + page.path("pageid").asText(),
                        page.path("title").asText(),
                        extract("it".equals(lang) ? CREATOR_IT : CREATOR_EN, description),
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
        if (!m.find()) {
            return null;
        }
        // "Edward Hopper del 1942" / "Edward Hopper in 1942": l'anno ha già il suo campo
        return m.group(1).replaceAll("\\s+(?:del|nel|in|from)\\s+\\d{4}.*$", "").trim();
    }

    private Integer extractYear(String text) {
        Matcher m = YEAR.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
