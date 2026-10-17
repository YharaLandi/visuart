package com.atlante;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatAndAiIntegrationTest extends AbstractIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    int port;

    @Value("${app.llm.max-tokens}")
    int maxTokens;

    private final WebSocketStompClient stomp = newClient();

    private static WebSocketStompClient newClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules());
        client.setMessageConverter(converter);
        return client;
    }

    @AfterEach
    void stop() {
        stomp.stop();
    }

    private StompSession connect(String token) throws Exception {
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }
        return stomp.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                headers, new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((Map<String, Object>) payload);
            }
        });
        return queue;
    }

    @Test
    void connectWithoutValidJwtIsRejected() {
        assertThatThrownBy(() -> connect(null)).isNotNull();
        assertThatThrownBy(() -> connect("token.non.valido")).isNotNull();
    }

    @Test
    void messageIsDeliveredInRealTimeStoredAndMarkedRead() throws Exception {
        JsonNode alice = register(unique("alice"));
        JsonNode bob = register(unique("bob"));
        String aliceName = alice.get("username").asText();
        String bobName = bob.get("username").asText();

        StompSession aliceSession = connect(alice.get("token").asText());
        StompSession bobSession = connect(bob.get("token").asText());
        BlockingQueue<Map<String, Object>> aliceInbox = subscribe(aliceSession, "/user/queue/messages");
        BlockingQueue<Map<String, Object>> bobInbox = subscribe(bobSession, "/user/queue/messages");
        BlockingQueue<Map<String, Object>> bobUpdates = subscribe(bobSession, "/user/queue/updates");
        BlockingQueue<Map<String, Object>> aliceUpdates = subscribe(aliceSession, "/user/queue/updates");
        Thread.sleep(300); // le SUBSCRIBE sono asincrone

        aliceSession.send("/app/chat.send", Map.of("recipient", bobName, "text", "Ciao Bob", "tempId", "tmp-1"));

        Map<String, Object> received = bobInbox.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.get("sender")).isEqualTo(aliceName);
        assertThat(received.get("text")).isEqualTo("Ciao Bob");
        assertThat(received.get("status")).isEqualTo("DELIVERED"); // Bob è online
        assertThat(received.get("tempId")).isNull();
        assertThat(received.get("sentAt")).isInstanceOf(String.class); // ISO-8601, non timestamp numerico

        Map<String, Object> echo = aliceInbox.poll(5, TimeUnit.SECONDS);
        assertThat(echo).isNotNull();
        assertThat(echo.get("tempId")).isEqualTo("tmp-1");

        // cronologia via REST, con un messaggio non letto per Bob
        mvc.perform(get("/api/chat/history").param("with", aliceName)
                        .header("Authorization", "Bearer " + bob.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.unread").value(1));

        Number conversationId = (Number) received.get("conversationId");
        bobSession.send("/app/chat.read", Map.of("conversationId", conversationId));

        Map<String, Object> readNotice = firstOfType(aliceUpdates, "READ");
        assertThat(readNotice).isNotNull();
        assertThat(readNotice.get("user")).isEqualTo(bobName);

        mvc.perform(get("/api/chat/conversations").header("Authorization", "Bearer " + bob.get("token").asText()))
                .andExpect(jsonPath("$[0].counterpart").value(aliceName))
                .andExpect(jsonPath("$[0].unread").value(0))
                .andExpect(jsonPath("$[0].lastText").value("Ciao Bob"));

        aliceSession.send("/app/chat.typing", Map.of("recipient", bobName));
        assertThat(firstOfType(bobUpdates, "TYPING")).isNotNull();
    }

    @Test
    void invalidFrameReturnsErrorOnlyToSender() throws Exception {
        JsonNode alice = register(unique("alice"));
        StompSession session = connect(alice.get("token").asText());
        BlockingQueue<Map<String, Object>> errors = subscribe(session, "/user/queue/errors");
        Thread.sleep(300);

        session.send("/app/chat.send", Map.of("recipient", "nessuno-esiste-" + unique(""), "text", "ciao"));

        Map<String, Object> error = errors.poll(5, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.get("error").toString()).contains("non trovato");
    }

    @Test
    void aiSuggestionUsesConversationHistoryAndMapsExternalErrors() throws Exception {
        JsonNode alice = register(unique("alice"));
        JsonNode bob = register(unique("bob"));
        String bobName = bob.get("username").asText();
        String aliceToken = "Bearer " + alice.get("token").asText();

        StompSession session = connect(alice.get("token").asText());
        session.send("/app/chat.send", Map.of("recipient", bobName, "text", "Hai visto Nighthawks di Hopper?"));
        Thread.sleep(500);

        MvcResult history = mvc.perform(get("/api/chat/history").param("with", bobName)
                .header("Authorization", aliceToken)).andReturn();
        long conversationId = mapper.readTree(history.getResponse().getContentAsString())
                .get("conversationId").asLong();
        String request = mapper.writeValueAsString(Map.of("conversationId", conversationId));

        llmStatus = 200;
        mvc.perform(post("/api/chat/suggest").header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion").value("Che ne pensi della palette di Hopper?"));
        assertThat(llmLastRequest).contains("Hai visto Nighthawks di Hopper?")
                .contains("\"max_tokens\":" + maxTokens);

        // un terzo utente non può usare l'AI sulla chat di altri
        JsonNode eve = register(unique("eve"));
        mvc.perform(post("/api/chat/suggest").header("Authorization", "Bearer " + eve.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());

        try {
            llmStatus = 429;
            llmRetryAfter = "12";
            llmBody = "{}";
            mvc.perform(post("/api/chat/suggest").header("Authorization", aliceToken)
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().string("Retry-After", "12"));

            llmStatus = 500;
            llmRetryAfter = null;
            mvc.perform(post("/api/chat/suggest").header("Authorization", aliceToken)
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadGateway());

            llmStatus = 401;
            mvc.perform(post("/api/chat/suggest").header("Authorization", aliceToken)
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadGateway());
        } finally {
            llmStatus = 200;
            llmRetryAfter = null;
            llmBody = completion("Che ne pensi della palette di Hopper?");
        }
    }

    private Map<String, Object> firstOfType(BlockingQueue<Map<String, Object>> queue, String type)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> next = queue.poll(500, TimeUnit.MILLISECONDS);
            if (next != null && type.equals(next.get("type"))) {
                return next;
            }
        }
        return null;
    }
}
