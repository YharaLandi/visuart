package com.atlante;

import com.atlante.model.Category;
import com.atlante.model.Work;
import com.atlante.model.WorkType;
import com.atlante.repository.CategoryRepository;
import com.atlante.repository.WorkRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Una connessione può accostare anche due film o due opere d'arte, non solo un film e un dipinto. */
class SameKindPairIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    WorkRepository works;
    @Autowired
    CategoryRepository categories;

    private String create(JsonNode user, Work a, Work b) throws Exception {
        categories.findBySlug("tema").orElseGet(() -> categories.save(new Category("Tema", "tema")));
        return """
                {"sourceWorkId":%d,"targetWorkId":%d,"categorySlug":"tema","criticalNote":"Due sguardi sulla stessa solitudine."}
                """.formatted(a.getId(), b.getId());
    }

    @Test
    void filmWithFilmAndArtworkWithArtwork() throws Exception {
        JsonNode user = register(unique("coppie"));
        String bearer = "Bearer " + user.get("token").asText();

        Work f1 = works.save(new Work(WorkType.FILM, "Film uno " + unique(""), "Regista", 1970));
        Work f2 = works.save(new Work(WorkType.FILM, "Film due " + unique(""), "Regista", 1990));
        mvc.perform(post("/api/connections").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(create(user, f1, f2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source.type").value("FILM"))
                .andExpect(jsonPath("$.target.type").value("FILM"));

        Work a1 = works.save(new Work(WorkType.ARTWORK, "Quadro uno " + unique(""), "Pittore", 1650));
        Work a2 = works.save(new Work(WorkType.ARTWORK, "Quadro due " + unique(""), "Pittore", 1890));
        mvc.perform(post("/api/connections").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(create(user, a1, a2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source.type").value("ARTWORK"))
                .andExpect(jsonPath("$.target.type").value("ARTWORK"));

        // resta vietato accostare un'opera a sé stessa
        mvc.perform(post("/api/connections").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(create(user, f1, f1)))
                .andExpect(status().isBadRequest());
    }
}
