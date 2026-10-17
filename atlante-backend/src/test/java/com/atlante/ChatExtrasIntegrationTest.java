package com.atlante;

import com.atlante.model.*;
import com.atlante.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatExtrasIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CategoryRepository categories;
    @Autowired
    WorkRepository works;
    @Autowired
    ConnectionRepository connections;
    @Autowired
    UserRepository users;

    private static String bearer(JsonNode auth) {
        return "Bearer " + auth.get("token").asText();
    }

    private org.springframework.test.web.servlet.ResultActions send(JsonNode from, String to, String text,
                                                                    Long connectionId) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(Map.of("recipient", to));
        if (text != null) body.put("text", text);
        if (connectionId != null) body.put("connectionId", connectionId);
        return mvc.perform(post("/api/chat/messages").header("Authorization", bearer(from))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    @Test
    void userSearchFindsOthersByNameWithoutExposingEmails() throws Exception {
        String tag = unique("ricerca");
        JsonNode me = register(tag + "a");
        register(tag + "b");

        String body = mvc.perform(get("/api/users/search").param("q", tag).header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(tag + "b"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("@test.local");

        mvc.perform(get("/api/users/search").param("q", "x").header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/users/search").param("q", tag)).andExpect(status().isForbidden());
    }

    @Test
    void connectionCanBeSharedInChatAndUnreadTotalIsTracked() throws Exception {
        JsonNode alice = register(unique("alice"));
        JsonNode bob = register(unique("bob"));
        String bobName = bob.get("username").asText();

        Category cat = categories.findBySlug("luce").orElseGet(() -> categories.save(new Category("Luce", "luce")));
        Connection c = connections.save(new Connection(
                works.save(new Work(WorkType.FILM, "Film " + unique(""), "R", 1990)),
                works.save(new Work(WorkType.ARTWORK, "Quadro " + unique(""), "P", 1600)),
                cat, "nota", users.findByUsername(alice.get("username").asText()).orElseThrow()));

        mvc.perform(get("/api/connections/" + c.getId()))
                .andExpect(jsonPath("$.authorUsername").value(alice.get("username").asText()));

        send(alice, bobName, null, c.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.connectionId").value(c.getId()));
        send(alice, bobName, "guarda questa", null).andExpect(status().isCreated());
        send(alice, bobName, null, 999_999L).andExpect(status().isNotFound());
        send(alice, bobName, " ", null).andExpect(status().isBadRequest());

        mvc.perform(get("/api/chat/unread-count").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.unread").value(2));
    }

    @Test
    void historyIsPagedBackwards() throws Exception {
        JsonNode alice = register(unique("alice"));
        JsonNode bob = register(unique("bob"));
        String bobName = bob.get("username").asText();
        for (int i = 1; i <= 35; i++) {
            send(alice, bobName, "msg " + i, null).andExpect(status().isCreated());
        }

        String first = mvc.perform(get("/api/chat/history").param("with", bobName).header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.messages.length()").value(30))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.messages[0].text").value("msg 6"))
                .andExpect(jsonPath("$.messages[29].text").value("msg 35"))
                .andReturn().getResponse().getContentAsString();
        long oldestId = mapper.readTree(first).get("messages").get(0).get("id").asLong();

        mvc.perform(get("/api/chat/history").param("with", bobName).param("before", String.valueOf(oldestId))
                        .header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.messages.length()").value(5))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.messages[0].text").value("msg 1"));
    }
}
