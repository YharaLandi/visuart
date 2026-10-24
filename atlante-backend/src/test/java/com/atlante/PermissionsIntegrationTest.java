package com.atlante;

import com.atlante.model.*;
import com.atlante.repository.*;
import com.atlante.service.MailService;
import com.atlante.service.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermissionsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    com.atlante.service.RoleService roleService;

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
    @Autowired
    MailService mailService;

    private static String bearer(JsonNode auth) {
        return "Bearer " + auth.get("token").asText();
    }

    private String adminToken() {
        String name = unique("admin");
        return "Bearer " + tokens.issue(users.save(User.builder().username(name).email(name + "@test.local")
                .passwordHash("x").displayName("Admin").roles(roleService.setOf(Role.ADMIN)).build()));
    }

    @Test
    void onlyAdminsCanEditOrDeleteWorksAndUsedWorksAreProtected() throws Exception {
        JsonNode curator = register(unique("curatore"));
        String admin = adminToken();
        Work free = works.save(new Work(WorkType.FILM, "Libera " + unique(""), "R", 2000));
        Work used = works.save(new Work(WorkType.FILM, "Usata " + unique(""), "R", 2000));
        Work art = works.save(new Work(WorkType.ARTWORK, "Quadro " + unique(""), "P", 1700));
        Category cat = categories.findBySlug("luce").orElseGet(() -> categories.save(new Category("Luce", "luce")));
        connections.save(new Connection(used, art, cat, "nota", null));
        String edit = mapper.writeValueAsString(Map.of("type", "FILM", "title", "Nuovo titolo"));

        mvc.perform(put("/api/works/" + free.getId()).header("Authorization", bearer(curator))
                .contentType(MediaType.APPLICATION_JSON).content(edit)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/works/" + free.getId()).header("Authorization", bearer(curator)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/works/" + free.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(edit))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Nuovo titolo"));

        mvc.perform(delete("/api/works/" + used.getId()).header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Opera usata in 1 connessioni: elimina prima quelle connessioni"));
        mvc.perform(delete("/api/works/" + free.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void commentsCanBeEditedAndDeletedOnlyByAuthorOrAdmin() throws Exception {
        JsonNode author = register(unique("autore"));
        JsonNode other = register(unique("altro"));
        Category cat = categories.findBySlug("luce").orElseGet(() -> categories.save(new Category("Luce", "luce")));
        Connection c = connections.save(new Connection(
                works.save(new Work(WorkType.FILM, "F " + unique(""), "R", 2000)),
                works.save(new Work(WorkType.ARTWORK, "Q " + unique(""), "P", 1700)), cat, "nota", null));
        String base = "/api/connections/" + c.getId() + "/comments";

        String created = mvc.perform(post(base).header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"prima versione\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUsername").value(author.get("username").asText()))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        mvc.perform(put(base + "/" + id).header("Authorization", bearer(other))
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"vandalo\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(base + "/" + id).header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"seconda versione\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("seconda versione"))
                .andExpect(jsonPath("$.editedAt").isNotEmpty());

        // un id valido ma di un'altra connessione risponde 404
        mvc.perform(delete("/api/connections/999999/comments/" + id).header("Authorization", bearer(author)))
                .andExpect(status().isNotFound());
        mvc.perform(delete(base + "/" + id).header("Authorization", bearer(other))).andExpect(status().isForbidden());
        mvc.perform(delete(base + "/" + id).header("Authorization", adminToken())).andExpect(status().isNoContent());
        mvc.perform(get(base)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void apiDocsArePublicAndTestsNeverSendRealMail() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("VisuArt — API"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        assertThat(mailService.isEnabled()).isFalse();
    }
}
