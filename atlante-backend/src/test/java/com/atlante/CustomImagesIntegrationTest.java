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
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Immagini scelte da chi scrive (fotogramma, dettaglio) e opere aggiunte a mano. */
class CustomImagesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    WorkRepository works;
    @Autowired
    CategoryRepository categories;

    private static byte[] png() throws Exception {
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 40; x++) for (int y = 0; y < 30; y++) img.setRGB(x, y, x < 20 ? 0x2a6f66 : 0xc2410c);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private String upload(String bearer) throws Exception {
        String body = mvc.perform(multipart("/api/uploads/image")
                        .file(new MockMultipartFile("file", "frame.png", "image/png", png()))
                        .header("Authorization", bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.palette").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("url").asText();
    }

    @Test
    void connectionKeepsTheChosenFrameAndDetail() throws Exception {
        JsonNode user = register(unique("fotogramma"));
        String bearer = "Bearer " + user.get("token").asText();
        categories.findBySlug("luce").orElseGet(() -> categories.save(new Category("Luce", "luce")));
        Work film = works.save(new Work(WorkType.FILM, "Film " + unique(""), "Regista", 1980));
        Work art = works.save(new Work(WorkType.ARTWORK, "Quadro " + unique(""), "Pittore", 1700));
        String frame = upload(bearer);
        String detail = upload(bearer);

        String body = mapper.writeValueAsString(Map.of("sourceWorkId", film.getId(), "targetWorkId", art.getId(),
                "categorySlug", "luce", "criticalNote", "La stessa luce radente.",
                "sourceImageUrl", frame, "targetImageUrl", detail));
        mvc.perform(post("/api/connections").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceImageUrl").value(frame))
                .andExpect(jsonPath("$.targetImageUrl").value(detail))
                // l'opera resta quella degli archivi
                .andExpect(jsonPath("$.source.title").value(film.getTitle()));

        // un URL esterno arbitrario non è accettato come immagine della connessione
        String bad = mapper.writeValueAsString(Map.of("sourceWorkId", film.getId(), "targetWorkId", art.getId(),
                "categorySlug", "luce", "criticalNote", "Nota.", "sourceImageUrl", "https://esempio.it/x.jpg"));
        mvc.perform(post("/api/connections").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void connectionCanCarryItsOwnPalette() throws Exception {
        JsonNode user = register(unique("palette"));
        String bearer = "Bearer " + user.get("token").asText();
        categories.findBySlug("colore").orElseGet(() -> categories.save(new Category("Colore", "colore")));
        Work film = works.save(new Work(WorkType.FILM, "Film " + unique(""), "Regista", 1980));
        Work art = works.save(new Work(WorkType.ARTWORK, "Quadro " + unique(""), "Pittore", 1700));
        String ok = mapper.writeValueAsString(Map.of("sourceWorkId", film.getId(), "targetWorkId", art.getId(),
                "categorySlug", "colore", "criticalNote", "Lo stesso rosso.",
                "sourcePalette", "#AA0000,#bb1111,#cc2222", "targetPalette", "#001122,#334455,#667788,#99aabb"));
        mvc.perform(post("/api/connections").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourcePalette").value("#aa0000,#bb1111,#cc2222"))
                .andExpect(jsonPath("$.targetPalette").value("#001122,#334455,#667788,#99aabb"));
        String bad = mapper.writeValueAsString(Map.of("sourceWorkId", film.getId(), "targetWorkId", art.getId(),
                "categorySlug", "colore", "criticalNote", "Nota.", "sourcePalette", "rosso,verde"));
        mvc.perform(post("/api/connections").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCanAddAWorkThatIsNotInTheArchives() throws Exception {
        JsonNode user = register(unique("opera"));
        String bearer = "Bearer " + user.get("token").asText();
        String image = upload(bearer);
        String body = mapper.writeValueAsString(Map.of("type", "ARTWORK", "title", "Affresco di paese " + unique(""),
                "creator", "Anonimo", "year", 1450, "description", "Cappella privata, non catalogata.", "imageUrl", image));
        mvc.perform(post("/api/works").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(image))
                .andExpect(jsonPath("$.palette").isNotEmpty())
                .andExpect(jsonPath("$.externalSource").value("MANUAL"));
    }

    @Test
    void uploadRejectsFakeImages() throws Exception {
        JsonNode user = register(unique("finto"));
        mvc.perform(multipart("/api/uploads/image")
                        .file(new MockMultipartFile("file", "x.jpg", "image/jpeg", "testo".getBytes()))
                        .header("Authorization", "Bearer " + user.get("token").asText()))
                .andExpect(status().isBadRequest());
    }
}
