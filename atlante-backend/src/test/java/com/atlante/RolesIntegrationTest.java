package com.atlante;

import com.atlante.model.Role;
import com.atlante.model.User;
import com.atlante.repository.UserRepository;
import com.atlante.service.RoleService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ruoli come dati: creati dall'admin, assegnati agli utenti, rispettati dai permessi. */
class RolesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    UserRepository users;
    @Autowired
    RoleService roleService;
    @Autowired
    PasswordEncoder encoder;

    private String adminBearer() throws Exception {
        String name = unique("capo");
        users.save(User.builder().username(name).email(name + "@test.local")
                .passwordHash(encoder.encode("Password12345")).displayName("Capo")
                .roles(roleService.setOf(Role.ADMIN)).build());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", name, "password", "Password12345"))))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("token").asText();
    }

    @Test
    void newUsersStartAsUserAndTheRoleIsRealData() throws Exception {
        JsonNode user = register(unique("nuovo"));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + user.get("token").asText()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void adminCreatesAssignsAndDeletesARole() throws Exception {
        String admin = adminBearer();
        String created = mvc.perform(post("/api/admin/roles").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "moderatore " + unique("").substring(0, 4).replaceAll("\\d", "x"), "description", "Modera i dialoghi"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.system").value(false))
                .andReturn().getResponse().getContentAsString();
        JsonNode role = mapper.readTree(created);
        String roleName = role.get("name").asText();

        JsonNode target = register(unique("utente"));
        Long targetId = users.findByUsername(target.get("username").asText()).orElseThrow().getId();
        mvc.perform(put("/api/admin/users/" + targetId + "/roles").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("roles", java.util.List.of(roleName, "CURATOR")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CURATOR"))
                .andExpect(jsonPath("$.roles.length()").value(3));

        // i ruoli di sistema non si eliminano; quelli creati sì, e chi li aveva li perde
        Long systemId = roleService.get(Role.USER).getId();
        mvc.perform(delete("/api/admin/roles/" + systemId).header("Authorization", admin)).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/roles/" + role.get("id").asLong()).header("Authorization", admin)).andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(users.findById(targetId).orElseThrow().getRoleNames())
                .containsExactly("CURATOR", "USER");
    }

    @Test
    void onlyAdminsManageRoles() throws Exception {
        JsonNode user = register(unique("curioso"));
        mvc.perform(get("/api/admin/roles").header("Authorization", "Bearer " + user.get("token").asText()))
                .andExpect(status().isForbidden());
    }
}
