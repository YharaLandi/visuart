package com.atlante;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvatarIntegrationTest extends AbstractIntegrationTest {

    /** Intestazione PNG minima: basta per il controllo dei primi byte. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    private static String bearer(JsonNode auth) {
        return "Bearer " + auth.get("token").asText();
    }

    @Test
    void uploadServeAndRemoveAvatar() throws Exception {
        String username = unique("foto");
        JsonNode user = register(username);

        // senza foto il profilo pubblico risponde 204 e hasAvatar è falso
        mvc.perform(get("/api/users/" + username + "/avatar")).andExpect(status().isNoContent());

        mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "me.png", "image/png", PNG))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAvatar").value(true));

        // la foto è pubblica: si legge anche senza accesso
        mvc.perform(get("/api/users/" + username + "/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mvc.perform(get("/api/users/" + username + "/profile"))
                .andExpect(jsonPath("$.hasAvatar").value(true));

        mvc.perform(delete("/api/users/me/avatar").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAvatar").value(false));
        mvc.perform(get("/api/users/" + username + "/avatar")).andExpect(status().isNoContent());
    }

    @Test
    void rejectsFilesThatAreNotImages() throws Exception {
        JsonNode user = register(unique("finto"));
        // dichiarato come PNG ma il contenuto è testo
        mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "x.png", "image/png", "non sono un'immagine".getBytes()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "x.gif", "image/gif", PNG))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadRequiresLogin() throws Exception {
        mvc.perform(multipart("/api/users/me/avatar")
                        .file(new MockMultipartFile("file", "me.png", "image/png", PNG)))
                .andExpect(status().isForbidden());
    }
}
