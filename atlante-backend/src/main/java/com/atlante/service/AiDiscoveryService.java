package com.atlante.service;

import com.atlante.client.AicClient;

import com.atlante.client.MetMuseumClient;
import com.atlante.client.TmdbClient;
import com.atlante.dto.Dtos.AiDiscoveryResponse;
import com.atlante.dto.Dtos.AiSuggestion;
import com.atlante.dto.Dtos.ExternalSearchResult;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.WorkType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scoperta assistita: dato un film (o un quadro) anche non presente nel
 * database, l'AI suggerisce accostamenti con l'altro tipo di opera su due
 * binari: trama/tema e palette cromatica.
 */
@Service
public class AiDiscoveryService {

    private final TmdbClient tmdb;
    private final MetMuseumClient met;
    private final AicClient aic;
    private final com.atlante.client.WikipediaClient wikipedia;
    private final LlmService llm;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, AiDiscoveryResponse> cache = new ConcurrentHashMap<>();

    public AiDiscoveryService(TmdbClient tmdb, MetMuseumClient met,
                              AicClient aic, com.atlante.client.WikipediaClient wikipedia, LlmService llm) {
        this.tmdb = tmdb;
        this.met = met;
        this.aic = aic;
        this.wikipedia = wikipedia;
        this.llm = llm;
    }

    public AiDiscoveryResponse discover(String query, WorkType type) {
        String key = type + ":" + query.trim().toLowerCase();
        AiDiscoveryResponse cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        AiDiscoveryResponse result = type == WorkType.FILM
                ? discoverForFilm(query)
                : discoverForArtwork(query);
        cache.put(key, result);
        return result;
    }

    private AiDiscoveryResponse discoverForFilm(String query) {
        ExternalSearchResult film = tmdb.search(query).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Film non trovato su TMDB: " + query));
        String palette = paletteFromUrl(film.imageUrl());

        String prompt = """
                Sei un critico cinematografico e d'arte. Dato il film "%s" (%s)%s,
                suggerisci esattamente 4 dipinti o opere d'arte visiva che vi si collegano:
                2 per affinità di TRAMA o tema, 2 per affinità di PALETTE cromatica.
                Scegli opere realmente esistenti e riconosciute.

                Rispondi SOLO con JSON valido, senza markdown:
                {"suggestions":[{"title":"...","creator":"...","year":1900,"linkType":"trama","reason":"..."}]}

                linkType: "trama" oppure "palette". reason: massimo 40 parole in italiano,
                tono critico colto ma accessibile.
                """.formatted(
                film.title(),
                film.year() != null ? film.year() : "anno n.d.",
                palette != null ? ", palette dominante " + palette : "");

        List<AiSuggestion> suggestions = parseSuggestions(llm.generate(prompt));
        suggestions = suggestions.stream()
                .map(s -> withImage(s, wikipedia.findImageUrl(s.title(), s.creator()).orElse(null)))
                .toList();

        return new AiDiscoveryResponse(film.title(), film.creator(), film.year(),
                film.imageUrl(), palette, suggestions);
    }

    private AiDiscoveryResponse discoverForArtwork(String query) {
        ExternalSearchResult art = met.search(query).stream().findFirst().orElse(null);
        String title = art != null ? art.title() : query;
        String creator = art != null ? art.creator() : null;
        Integer year = art != null ? art.year() : null;
        String imageUrl = art != null ? art.imageUrl()
                : wikipedia.findImageUrl(query, null).orElse(null);
        String palette = paletteFromUrl(imageUrl);

        String prompt = """
                Sei un critico cinematografico e d'arte. Data l'opera d'arte "%s"%s%s,
                suggerisci esattamente 4 film che vi si collegano:
                2 per affinità di TRAMA o tema, 2 per affinità di PALETTE cromatica.
                Scegli film realmente esistenti e riconosciuti.

                Rispondi SOLO con JSON valido, senza markdown:
                {"suggestions":[{"title":"...","creator":"regista","year":2000,"linkType":"palette","reason":"..."}]}

                linkType: "trama" oppure "palette". reason: massimo 40 parole in italiano,
                tono critico colto ma accessibile.
                """.formatted(
                title,
                creator != null ? " di " + creator : "",
                palette != null ? ", palette dominante " + palette : "");

        List<AiSuggestion> suggestions = parseSuggestions(llm.generate(prompt));
        suggestions = suggestions.stream()
                .map(s -> withImage(s, posterFor(s.title())))
                .toList();

        return new AiDiscoveryResponse(title, creator, year, imageUrl, palette, suggestions);
    }

    private String posterFor(String filmTitle) {
        try {
            if (!tmdb.isConfigured()) {
                return null;
            }
            return tmdb.search(filmTitle).stream().findFirst()
                    .map(ExternalSearchResult::imageUrl).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private AiSuggestion withImage(AiSuggestion s, String imageUrl) {
        return new AiSuggestion(s.title(), s.creator(), s.year(),
                s.linkType(), s.reason(), imageUrl);
    }

    private List<AiSuggestion> parseSuggestions(String raw) {
        String cleaned = raw.trim()
                .replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        List<AiSuggestion> out = new ArrayList<>();
        try {
            JsonNode arr = mapper.readTree(cleaned).path("suggestions");
            for (JsonNode n : arr) {
                out.add(new AiSuggestion(
                        n.path("title").asText(),
                        n.path("creator").asText(null),
                        n.hasNonNull("year") ? n.get("year").asInt() : null,
                        n.path("linkType").asText("trama"),
                        n.path("reason").asText(""),
                        null));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Risposta AI non interpretabile, riprova");
        }
        return out;
    }

    private String paletteFromUrl(String url) {
        if (url == null) {
            return null;
        }
        Path tmp = null;
        try {
            tmp = Files.createTempFile("atlante-discover", ".img");
            var conn = URI.create(url).toURL().openConnection();
            conn.setRequestProperty("User-Agent",
                    "VisuArt/1.0 (progetto didattico Epicode)");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(20_000);
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            return PaletteExtractor.extract(tmp, 4);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (tmp != null) {
                    Files.deleteIfExists(tmp);
                }
            } catch (Exception ignored) {
                // pulizia best-effort del file temporaneo
            }
        }
    }
}
