package com.atlante;

import com.atlante.model.Role;
import com.atlante.model.Work;
import com.atlante.model.WorkType;
import com.atlante.repository.UserRepository;
import com.atlante.repository.WorkRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reparto fantasma: regole sul true crime, link solo a Wikipedia, parole sensibili, permessi. */
class PhantomIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    WorkRepository works;
    @Autowired
    UserRepository users;
    @Autowired
    com.atlante.service.RoleService roleService;

    private String curator() throws Exception {
        JsonNode u = register(unique("curatrice"));
        var user = users.findByUsername(u.get("username").asText()).orElseThrow();
        user.getRoles().add(roleService.get(Role.CURATOR));
        users.save(user);
        // il filtro JWT rilegge il ruolo dal database a ogni richiesta
        return "Bearer " + u.get("token").asText();
    }

    private String body(Long filmId, String type, String trivia, String url) throws Exception {
        Map<String, Object> m = new HashMap<>(Map.of("filmId", filmId, "type", type,
                "originTitle", "Origine di prova", "wikipediaUrl", url));
        if (trivia != null) m.put("trivia", trivia);
        return mapper.writeValueAsString(m);
    }

    private org.springframework.test.web.servlet.ResultActions send(String bearer, String json) throws Exception {
        return mvc.perform(post("/api/phantom").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void rulesForOriginsAreEnforcedByTheServer() throws Exception {
        String bearer = curator();
        Long film = works.save(new Work(WorkType.FILM, "Horror " + unique(""), "Regista", 1980)).getId();
        String wiki = "https://it.wikipedia.org/wiki/Leggenda";

        send(bearer, body(film, "LEGGENDA", "Una leggenda di paese raccontata davanti al camino.", wiki))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trivia").isNotEmpty());

        // true crime: solo il link, nessun racconto
        send(bearer, body(film, "TRUE_CRIME", "Qualche dettaglio sul caso.", wiki)).andExpect(status().isBadRequest());
        send(bearer, body(film, "TRUE_CRIME", null, wiki)).andExpect(status().isCreated());

        // fonti: solo Wikipedia italiana o inglese
        send(bearer, body(film, "MITO", null, "https://sito-qualunque.it/storia")).andExpect(status().isBadRequest());

        // parole sensibili, anche con maiuscole e accenti
        send(bearer, body(film, "LEGGENDA", "Le VÌTTIME del paese.", wiki)).andExpect(status().isBadRequest());

        mvc.perform(get("/api/phantom")).andExpect(status().isOk());
    }

    @Test
    void onlyCuratorsWriteOrigins() throws Exception {
        JsonNode user = register(unique("lettore"));
        Long film = works.save(new Work(WorkType.FILM, "Horror " + unique(""), "Regista", 1990)).getId();
        send("Bearer " + user.get("token").asText(), body(film, "MITO", null, "https://en.wikipedia.org/wiki/Myth"))
                .andExpect(status().isForbidden());
    }
}
