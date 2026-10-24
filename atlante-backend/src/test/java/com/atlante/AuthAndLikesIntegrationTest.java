package com.atlante;

import com.atlante.model.Category;
import com.atlante.model.Connection;
import com.atlante.model.Role;
import com.atlante.model.User;
import com.atlante.model.Work;
import com.atlante.model.WorkType;
import com.atlante.repository.CategoryRepository;
import com.atlante.repository.ConnectionRepository;
import com.atlante.repository.UserRepository;
import com.atlante.repository.WorkRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthAndLikesIntegrationTest extends AbstractIntegrationTest {

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

    private Connection newConnection(String authorUsername) {
        Category category = categories.findBySlug("luce")
                .orElseGet(() -> categories.save(new Category("Luce", "luce")));
        Work a = works.save(new Work(WorkType.FILM, "Film " + unique(""), "Regista", 1980));
        Work b = works.save(new Work(WorkType.ARTWORK, "Quadro " + unique(""), "Pittore", 1650));
        User author = users.findByUsername(authorUsername).orElseThrow();
        return connections.save(new Connection(a, b, category, "nota critica", author));
    }

    private String bearer(JsonNode auth) {
        return "Bearer " + auth.get("token").asText();
    }

    @Test
    void registerThenLoginWithEmailOrUsername() throws Exception {
        String username = unique("mario");
        JsonNode registered = register(username);
        assertThat(registered.get("token").asText()).isNotBlank();
        assertThat(registered.get("username").asText()).isEqualTo(username);
        assertThat(registered.get("role").asText()).isEqualTo("USER");

        for (String identifier : new String[]{username + "@test.local", username}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("email", identifier, "password", "Password12345"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }
    }

    @Test
    void loginWithWrongPasswordIs401AndDuplicateRegistrationIs400() throws Exception {
        String username = unique("luigi");
        register(username);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", username, "password", "sbagliata"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", username + "@test.local",
                                "password", "Password12345", "displayName", "Dup"))))
                .andExpect(status().isBadRequest());

        // la stessa email con maiuscole diverse è comunque un duplicato
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", username.toUpperCase() + "@TEST.local",
                                "password", "Password12345", "displayName", "Dup"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email già registrata"));
    }

    @Test
    void registrationWithoutUsernameDerivesOneFromEmail() throws Exception {
        String local = unique("derived");
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", local + "@test.local",
                                "password", "Password12345", "displayName", "Derivato"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(body).get("username").asText()).isEqualTo(local);
    }

    @Test
    void likeLifecycle() throws Exception {
        JsonNode user = register(unique("fan"));
        Connection connection = newConnection(user.get("username").asText());
        String url = "/api/connections/" + connection.getId() + "/likes";

        mvc.perform(post(url)).andExpect(status().isForbidden());

        mvc.perform(post(url).header("Authorization", bearer(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));

        mvc.perform(post(url).header("Authorization", bearer(user)))
                .andExpect(status().isConflict());

        // il conteggio compare nella connessione, e likedByMe dipende da chi guarda
        mvc.perform(get("/api/connections/" + connection.getId()).header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));
        mvc.perform(get("/api/connections/" + connection.getId()))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));

        mvc.perform(delete(url).header("Authorization", bearer(user))).andExpect(status().isNoContent());
        mvc.perform(delete(url).header("Authorization", bearer(user))).andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    void onlyAuthorOrAdminCanEditOrDeleteAConnection() throws Exception {
        JsonNode author = register(unique("autore"));
        JsonNode other = register(unique("altro"));
        Connection connection = newConnection(author.get("username").asText());
        String url = "/api/connections/" + connection.getId();
        String edit = mapper.writeValueAsString(Map.of("sourceWorkId", connection.getSource().getId(),
                "targetWorkId", connection.getTarget().getId(), "categorySlug", "luce",
                "criticalNote", "nota aggiornata"));

        mvc.perform(put(url).header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON).content(edit))
                .andExpect(status().isForbidden());
        mvc.perform(delete(url).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        mvc.perform(put(url).header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON).content(edit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criticalNote").value("nota aggiornata"));

        // un admin può moderare anche le connessioni altrui
        String adminName = unique("admin");
        users.save(User.builder().username(adminName).email(adminName + "@test.local")
                .passwordHash("x").displayName("Admin").roles(roleService.setOf(Role.ADMIN)).build());
        String adminToken = "Bearer " + loginToken(adminName);
        mvc.perform(delete(url).header("Authorization", adminToken)).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    private String loginToken(String username) {
        return tokens.issue(users.findByUsername(username).orElseThrow());
    }

    @Autowired
    com.atlante.service.TokenService tokens;
}
