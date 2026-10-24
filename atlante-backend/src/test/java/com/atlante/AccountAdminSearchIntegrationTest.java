package com.atlante;

import com.atlante.model.*;
import com.atlante.repository.*;
import com.atlante.service.MailService;
import com.atlante.service.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountAdminSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    com.atlante.service.RoleService roleService;

    @LocalServerPort
    int port;
    @Autowired
    CategoryRepository categories;
    @Autowired
    WorkRepository works;
    @Autowired
    ConnectionRepository connections;
    @Autowired
    UserRepository users;
    @Autowired
    TokenService tokens;
    @MockitoSpyBean
    MailService mailService;

    private Connection newConnection(String authorUsername, String title, String note) {
        Category category = categories.findBySlug("luce")
                .orElseGet(() -> categories.save(new Category("Luce", "luce")));
        Work a = works.save(new Work(WorkType.FILM, title, "Regista", 1980));
        Work b = works.save(new Work(WorkType.ARTWORK, "Quadro " + unique(""), "Pittore", 1650));
        return connections.save(new Connection(a, b, category, note,
                users.findByUsername(authorUsername).orElseThrow()));
    }

    private static String bearer(JsonNode auth) {
        return "Bearer " + auth.get("token").asText();
    }

    @Test
    void logoutRevokesTokenAndRefreshIssuesANewOne() throws Exception {
        JsonNode user = register(unique("sessione"));
        mvc.perform(get("/api/users/me").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.get("username").asText()));

        String refreshed = mvc.perform(post("/api/auth/refresh").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newToken = mapper.readTree(refreshed).get("token").asText();

        // il token vecchio è stato revocato dal refresh
        mvc.perform(get("/api/users/me").header("Authorization", bearer(user))).andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileUpdatePasswordChangeAndStatsEmail() throws Exception {
        JsonNode user = register(unique("profilo"));
        String username = user.get("username").asText();

        mvc.perform(put("/api/users/me").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("displayName", "Nuovo Nome"))))
                .andExpect(jsonPath("$.displayName").value("Nuovo Nome"));

        mvc.perform(put("/api/users/me/password").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("currentPassword", "sbagliata", "newPassword", "NuovaPass1234"))))
                .andExpect(status().isUnauthorized());

        String body = mvc.perform(put("/api/users/me/password").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("currentPassword", "Password12345", "newPassword", "NuovaPass1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newToken = mapper.readTree(body).get("token").asText();
        // tutte le altre sessioni sono chiuse, quella nuova funziona
        mvc.perform(get("/api/users/me").header("Authorization", bearer(user))).andExpect(status().isForbidden());

        newConnection(username, "Film " + unique(""), "nota");
        mvc.perform(get("/api/users/me/stats").header("Authorization", "Bearer " + newToken))
                .andExpect(jsonPath("$.connections").value(1))
                .andExpect(jsonPath("$.likesReceived").value(0));

        mvc.perform(post("/api/users/me/stats/email").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isAccepted());
        verify(mailService, timeout(5000)).sendStats(eq(username + "@test.local"), eq("Nuovo Nome"), any());
    }

    @Test
    void deletingAccountRemovesEverythingItOwns() throws Exception {
        JsonNode owner = register(unique("addio"));
        JsonNode other = register(unique("rimane"));
        Connection mine = newConnection(owner.get("username").asText(), "Film " + unique(""), "nota");
        Connection theirs = newConnection(other.get("username").asText(), "Film " + unique(""), "nota");

        mvc.perform(post("/api/connections/" + theirs.getId() + "/likes").header("Authorization", bearer(owner)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/connections/" + mine.getId() + "/likes").header("Authorization", bearer(other)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/connections/" + theirs.getId() + "/comments").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"bello\"}"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/users/me").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"sbagliata\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/users/me").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"Password12345\"}"))
                .andExpect(status().isNoContent());

        assertThat(users.findByUsername(owner.get("username").asText())).isEmpty();
        mvc.perform(get("/api/connections/" + mine.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/connections/" + theirs.getId())).andExpect(jsonPath("$.likeCount").value(0));
        mvc.perform(get("/api/connections/" + theirs.getId() + "/comments")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void likeAndCommentNotifyTheAuthorInRealTime() throws Exception {
        JsonNode author = register(unique("autrice"));
        JsonNode fan = register(unique("fan"));
        Connection connection = newConnection(author.get("username").asText(), "Film " + unique(""), "nota");

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
        client.setMessageConverter(converter);
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", bearer(author));
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                headers, new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
        BlockingQueue<Map<String, Object>> inbox = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders h) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders h, Object payload) {
                inbox.add((Map<String, Object>) payload);
            }
        });
        Thread.sleep(300);

        try {
            mvc.perform(post("/api/connections/" + connection.getId() + "/likes").header("Authorization", bearer(fan)))
                    .andExpect(status().isCreated());
            Map<String, Object> like = inbox.poll(5, TimeUnit.SECONDS);
            assertThat(like).isNotNull();
            assertThat(like.get("type")).isEqualTo("LIKE");
            assertThat(like.get("actor")).isEqualTo(fan.get("username").asText());

            mvc.perform(post("/api/connections/" + connection.getId() + "/comments").header("Authorization", bearer(fan))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Accostamento perfetto\"}"))
                    .andExpect(status().isCreated());
            Map<String, Object> comment = inbox.poll(5, TimeUnit.SECONDS);
            assertThat(comment).isNotNull();
            assertThat(comment.get("preview")).isEqualTo("Accostamento perfetto");

            // le proprie azioni non generano notifiche
            mvc.perform(post("/api/connections/" + connection.getId() + "/likes").header("Authorization", bearer(author)))
                    .andExpect(status().isCreated());
            assertThat(inbox.poll(700, TimeUnit.MILLISECONDS)).isNull();

            mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(author)))
                    .andExpect(jsonPath("$.unread").value(2));
            mvc.perform(post("/api/notifications/read-all").header("Authorization", bearer(author)))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/api/notifications").header("Authorization", bearer(author)))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].read").value(true));
        } finally {
            client.stop();
        }
    }

    @Test
    void searchIsPagedFilteredAndSorted() throws Exception {
        JsonNode user = register(unique("cercatore"));
        String tag = unique("zzq");
        for (int i = 0; i < 5; i++) {
            newConnection(user.get("username").asText(), tag + " film " + i, "nota " + i);
        }

        mvc.perform(get("/api/connections/search").param("q", tag.toUpperCase()).param("size", "2")
                        .param("sort", "oldest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].source.title").value(tag + " film 0"));

        mvc.perform(get("/api/connections/search").param("q", tag).param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(1));

        mvc.perform(get("/api/connections/search").param("author", user.get("username").asText()).param("type", "ARTWORK"))
                .andExpect(jsonPath("$.totalElements").value(5));

        mvc.perform(get("/api/connections/search").param("sort", "password"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminManagesUsersAndOthersCannot() throws Exception {
        JsonNode normal = register(unique("normale"));
        JsonNode target = register(unique("bersaglio"));
        String adminName = unique("capo");
        User admin = users.save(User.builder().username(adminName).email(adminName + "@test.local")
                .passwordHash("x").displayName("Capo").roles(roleService.setOf(Role.ADMIN)).build());
        String adminToken = "Bearer " + tokens.issue(admin);
        Long targetId = users.findByUsername(target.get("username").asText()).orElseThrow().getId();

        mvc.perform(get("/api/admin/users").header("Authorization", bearer(normal))).andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/users").param("q", target.get("username").asText())
                        .header("Authorization", adminToken))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(target.get("username").asText() + "@test.local"));

        mvc.perform(patch("/api/admin/users/" + targetId + "/role").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        // il cambio di ruolo chiude le sessioni dell'utente
        mvc.perform(get("/api/users/me").header("Authorization", bearer(target))).andExpect(status().isForbidden());

        mvc.perform(patch("/api/admin/users/" + admin.getId() + "/role").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"CURATOR\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(delete("/api/admin/users/" + targetId).header("Authorization", adminToken))
                .andExpect(status().isNoContent());
        assertThat(users.findById(targetId)).isEmpty();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
