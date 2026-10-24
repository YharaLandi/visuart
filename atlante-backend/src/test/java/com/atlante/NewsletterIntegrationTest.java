package com.atlante;

import com.atlante.model.Role;
import com.atlante.model.User;
import com.atlante.repository.UserRepository;
import com.atlante.service.NewsletterService;
import com.atlante.service.RoleService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Newsletter: iscrizione di default, uscita dalle impostazioni o dal link firmato, anteprima per l'admin. */
class NewsletterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    UserRepository users;
    @Autowired
    NewsletterService newsletter;
    @Autowired
    RoleService roleService;
    @Autowired
    PasswordEncoder encoder;

    @Test
    void newUsersAreSubscribedAndCanLeave() throws Exception {
        JsonNode user = register(unique("lettore"));
        String bearer = "Bearer " + user.get("token").asText();
        mvc.perform(get("/api/users/me").header("Authorization", bearer)).andExpect(jsonPath("$.newsletter").value(true));
        mvc.perform(put("/api/users/me/newsletter").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(jsonPath("$.newsletter").value(false));
    }

    @Test
    void unsubscribeLinkNeedsTheRightSignature() throws Exception {
        JsonNode user = register(unique("distratto"));
        Long id = users.findByUsername(user.get("username").asText()).orElseThrow().getId();
        mvc.perform(get("/api/newsletter/unsubscribe").param("u", id.toString()).param("t", "falso"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("non è valido")));
        assertThat(users.findById(id).orElseThrow().isNewsletter()).isTrue();

        mvc.perform(get("/api/newsletter/unsubscribe").param("u", id.toString()).param("t", newsletter.token(id)))
                .andExpect(status().isOk());
        assertThat(users.findById(id).orElseThrow().isNewsletter()).isFalse();
    }

    @Test
    void adminPreviewsTheWeeklyChart() throws Exception {
        String name = unique("redattore");
        users.save(User.builder().username(name).email(name + "@test.local")
                .passwordHash(encoder.encode("Password12345")).displayName("Redazione")
                .roles(roleService.setOf(Role.ADMIN)).build());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", name, "password", "Password12345"))))
                .andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + mapper.readTree(body).get("token").asText();

        String html = mvc.perform(get("/api/admin/newsletter/preview").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Redazione").contains("/api/newsletter/unsubscribe?u=");
        Files.writeString(Path.of("target", "newsletter-preview.html"), html);

        JsonNode reader = register(unique("curioso"));
        mvc.perform(get("/api/admin/newsletter/preview").header("Authorization", "Bearer " + reader.get("token").asText()))
                .andExpect(status().isForbidden());
    }
}
