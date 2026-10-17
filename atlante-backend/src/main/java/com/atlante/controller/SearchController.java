package com.atlante.controller;

import com.atlante.client.AicClient;
import com.atlante.client.MetMuseumClient;
import com.atlante.client.TmdbClient;
import com.atlante.client.WikipediaClient;
import com.atlante.dto.Dtos.ExternalSearchResult;
import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.service.ImportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final TmdbClient tmdbClient;
    private final MetMuseumClient metClient;
    private final AicClient aicClient;
    private final WikipediaClient wikipediaClient;
    private final ImportService importService;

    public SearchController(TmdbClient tmdbClient, MetMuseumClient metClient,
                            AicClient aicClient, WikipediaClient wikipediaClient,
                            ImportService importService) {
        this.tmdbClient = tmdbClient;
        this.metClient = metClient;
        this.aicClient = aicClient;
        this.wikipediaClient = wikipediaClient;
        this.importService = importService;
    }

    @GetMapping("/search/artworks")
    public List<ExternalSearchResult> searchArtworks(@RequestParam String q) {
        List<ExternalSearchResult> results = wikipediaClient.search(q);
        if (results.isEmpty()) {
            results = aicClient.search(q);
        }
        if (results.isEmpty()) {
            results = metClient.search(q);
        }
        return results;
    }

    @GetMapping("/search/tmdb")
    public List<ExternalSearchResult> searchTmdb(@RequestParam String q) {
        return tmdbClient.search(q);
    }

    @GetMapping("/search/met")
    public List<ExternalSearchResult> searchMet(@RequestParam String q) {
        return metClient.search(q);
    }

    @PostMapping("/import/external")
    public com.atlante.dto.Dtos.WorkResponse importExternal(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
            com.atlante.dto.Dtos.ExternalImportRequest req) {
        return importService.importExternalResult(req);
    }

    @PostMapping("/import/tmdb/{tmdbId}")
    public WorkResponse importTmdb(@PathVariable String tmdbId) {
        return importService.importFromTmdb(tmdbId);
    }

    @PostMapping("/import/met/{objectId}")
    public WorkResponse importMet(@PathVariable String objectId) {
        return importService.importFromMet(objectId);
    }
}
