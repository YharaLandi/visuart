package com.atlante.service;

import com.atlante.dto.LlmRequest;
import com.atlante.dto.LlmResponse;
import com.atlante.exception.ExternalRequestInvalidException;
import com.atlante.exception.RateLimitException;
import com.atlante.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Unico punto di uscita verso il modello linguistico. Tutte le funzioni AI
 * (analisi cromatica, scoperta, suggerimento di risposta in chat) passano di qui.
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public LlmService(RestClient llmClient,
                      @Value("${app.llm.api-key:}") String apiKey,
                      @Value("${app.llm.model}") String model,
                      @Value("${app.llm.max-tokens}") int maxTokens) {
        this.client = llmClient;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generate(String prompt) {
        return generate(null, prompt);
    }

    public String generate(String instructions, String prompt) {
        if (!isConfigured()) {
            throw new IllegalArgumentException(
                    "AI non configurata: imposta la chiave in application.yml (app.llm.api-key)");
        }

        LlmRequest body = LlmRequest.of(model, maxTokens, instructions, prompt);

        long start = System.nanoTime();
        LlmResponse response = call(body);
        long millis = (System.nanoTime() - start) / 1_000_000;

        log.info("richiesta AI modello={} in={} out={} stop={} durata={}ms",
                response.model(), response.inputTokens(), response.outputTokens(),
                response.finishReason(), millis);

        String text = response.firstText();
        if (text.isBlank()) {
            log.warn("risposta AI senza testo: stop={} - se stop=length alzare app.llm.max-tokens",
                    response.finishReason());
            throw new ServiceUnavailableException("il modello non ha prodotto una risposta");
        }
        return text;
    }

    private LlmResponse call(LlmRequest body) {
        try {
            LlmResponse response = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        String retry = resp.getHeaders().getFirst("retry-after");
                        log.warn("429 dal servizio AI, retry-after={}", retry);
                        throw new RateLimitException(retry);
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.error("4xx dal servizio AI: {} - corpo: {}", resp.getStatusCode(), bodyOf(resp));
                        throw new ExternalRequestInvalidException(
                                "il servizio AI ha rifiutato la richiesta: " + resp.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("5xx dal servizio AI: {} - corpo: {}", resp.getStatusCode(), bodyOf(resp));
                        throw new ServiceUnavailableException(
                                "il servizio AI non ha risposto correttamente: " + resp.getStatusCode());
                    })
                    .body(LlmResponse.class);
            if (response == null) {
                throw new ServiceUnavailableException("nessuna risposta dal servizio AI");
            }
            return response;
        } catch (ResourceAccessException ex) {
            log.error("il servizio AI non e' raggiungibile: {}", ex.getMessage());
            throw new ServiceUnavailableException("il servizio AI non e' raggiungibile");
        }
    }

    private String bodyOf(ClientHttpResponse response) {
        try (var stream = response.getBody()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "(corpo non leggibile: " + ex.getMessage() + ")";
        }
    }
}
