package com.atlante;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base dei test di integrazione: applicazione completa con H2 e un finto servizio AI
 * (server HTTP locale) al posto di Gemini, controllabile con {@link #llmStatus} e {@link #llmBody}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static volatile int llmStatus = 200;
    protected static volatile String llmBody = completion("Che ne pensi della palette di Hopper?");
    protected static volatile String llmLastRequest;
    protected static volatile String llmRetryAfter;

    private static final HttpServer LLM_STUB = startLlmStub();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper mapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.llm.base-url", () -> "http://localhost:" + LLM_STUB.getAddress().getPort());
        registry.add("app.llm.api-key", () -> "test-key");
    }

    protected static String completion(String text) {
        return "{\"id\":\"x\",\"model\":\"stub\",\"choices\":[{\"index\":0,"
                + "\"message\":{\"role\":\"assistant\",\"content\":\"" + text + "\"},"
                + "\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":7,\"total_tokens\":12}}";
    }

    private static HttpServer startLlmStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/chat/completions", exchange -> {
                llmLastRequest = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] body = llmBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                if (llmRetryAfter != null) {
                    exchange.getResponseHeaders().add("Retry-After", llmRetryAfter);
                }
                exchange.sendResponseHeaders(llmStatus, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Registra un nuovo utente e restituisce la risposta di login (token, username, ...). */
    protected JsonNode register(String username) throws Exception {
        String json = mapper.writeValueAsString(java.util.Map.of(
                "email", username + "@test.local",
                "password", "Password12345",
                "displayName", "Utente " + username,
                "username", username));
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }
}
