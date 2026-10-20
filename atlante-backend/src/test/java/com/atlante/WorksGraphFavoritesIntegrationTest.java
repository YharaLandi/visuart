package com.atlante;

import com.atlante.model.*;
import com.atlante.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opere, grafo, preferiti, categorie e lettura AI di una connessione. */
class WorksGraphFavoritesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CategoryRepository categories;
    @Autowired
    WorkRepository works;
    @Autowired
    ConnectionRepository connections;

    private static String bearer(JsonNode auth) {
        return "Bearer " + auth.get("token").asText();
    }

    private Connection link(Work a, Work b) {
        Category cat = categories.findBySlug("luce").orElseGet(() -> categories.save(new Category("Luce", "luce")));
        return connections.save(new Connection(a, b, cat, "Una luce che separa. Seconda frase.", null));
    }

    @Test
    void worksCanBeCreatedByUsersAndSearchedByEveryone() throws Exception {
        JsonNode user = register(unique("autrice"));
        String title = unique("Film di prova ");

        mvc.perform(post("/api/works").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("type", "FILM", "title", title))))
                .andExpect(status().isForbidden());

        String body = mvc.perform(post("/api/works").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("type", "FILM", "title", title, "creator", "Regista", "year", 1999))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).get("id").asLong();

        mvc.perform(get("/api/works/" + id)).andExpect(jsonPath("$.title").value(title));
        mvc.perform(get("/api/works").param("q", title)).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/works/999999")).andExpect(status().isNotFound());
        mvc.perform(post("/api/works").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"FILM\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void graphReturnsNodesAndEdgesAroundAWork() throws Exception {
        Work film = works.save(new Work(WorkType.FILM, unique("F "), "R", 2000));
        Work art = works.save(new Work(WorkType.ARTWORK, unique("Q "), "P", 1700));
        Work far = works.save(new Work(WorkType.FILM, unique("Lontano "), "R", 2010));
        Connection near = link(film, art);
        link(far, art);

        mvc.perform(get("/api/graph").param("workId", String.valueOf(film.getId())).param("depth", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(2))
                .andExpect(jsonPath("$.edges[0].id").value(near.getId()));

        mvc.perform(get("/api/graph").param("workId", String.valueOf(film.getId())).param("depth", "2"))
                .andExpect(jsonPath("$.nodes.length()").value(3))
                .andExpect(jsonPath("$.edges.length()").value(2));
    }

    @Test
    void favoritesArePersonalAndIdempotent() throws Exception {
        JsonNode user = register(unique("collezionista"));
        Work art = works.save(new Work(WorkType.ARTWORK, unique("Quadro "), "P", 1650));
        String url = "/api/favorites/" + art.getId();

        mvc.perform(post(url)).andExpect(status().isForbidden());
        mvc.perform(post(url).header("Authorization", bearer(user))).andExpect(status().isCreated());
        mvc.perform(post(url).header("Authorization", bearer(user))).andExpect(status().isCreated());
        mvc.perform(get("/api/favorites").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(art.getId()));

        JsonNode other = register(unique("altro"));
        mvc.perform(get("/api/favorites").header("Authorization", bearer(other)))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(delete(url).header("Authorization", bearer(user))).andExpect(status().isNoContent());
        mvc.perform(get("/api/favorites").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void categoriesArePublicAndAiReadingUsesTheModel() throws Exception {
        categories.findBySlug("luce").orElseGet(() -> categories.save(new Category("Luce", "luce")));
        mvc.perform(get("/api/categories")).andExpect(status().isOk());

        Connection c = link(works.save(new Work(WorkType.FILM, unique("F "), "R", 2000)),
                works.save(new Work(WorkType.ARTWORK, unique("Q "), "P", 1700)));
        llmBody = completion("{\\\"filmThemes\\\":[\\\"notturno\\\"],\\\"artworkThemes\\\":[\\\"urbano\\\"],\\\"comparison\\\":\\\"Due notti.\\\"}");
        try {
            mvc.perform(get("/api/connections/" + c.getId() + "/ai-analysis"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.filmThemes[0]").value("notturno"))
                    .andExpect(jsonPath("$.comparison").value("Due notti."));
        } finally {
            llmBody = completion("Che ne pensi della palette di Hopper?");
        }
    }
}
