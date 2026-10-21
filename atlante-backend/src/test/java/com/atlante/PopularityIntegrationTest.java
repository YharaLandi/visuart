package com.atlante;

import com.atlante.model.*;
import com.atlante.repository.*;
import com.atlante.service.PopularityScore;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PopularityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    WorkRepository works;
    @Autowired
    CategoryRepository categories;
    @Autowired
    ConnectionRepository connections;
    @Autowired
    UserRepository users;
    @Autowired
    LikeRepository likes;
    @Autowired
    CommentRepository comments;

    @Test
    void interactionsOutweighViewsAndTimeDecaysGently() {
        Instant now = Instant.now();
        double discussed = PopularityScore.of(10, 4, 100, now, now);
        double onlyViewed = PopularityScore.of(0, 0, 50_000, now, now);
        assertThat(discussed).isGreaterThan(onlyViewed);
        // a parità di interazioni la più recente sta sopra, ma senza crolli
        double old = PopularityScore.of(10, 4, 100, now.minus(60, ChronoUnit.DAYS), now);
        assertThat(discussed).isGreaterThan(old);
        assertThat(old).isGreaterThan(discussed / 4);
    }

    @Test
    void popularSortPutsTheMostInteractedFirst() throws Exception {
        Category cat = categories.findBySlug("luce").orElseGet(() -> categories.save(new Category("Luce", "luce")));
        String tag = unique("pop");
        JsonNode author = register(unique("autore"));
        User a = users.findByUsername(author.get("username").asText()).orElseThrow();
        Connection quiet = connections.save(new Connection(
                works.save(new Work(WorkType.FILM, tag + " quieto", "R", 1990)),
                works.save(new Work(WorkType.ARTWORK, tag + " quadro quieto", "P", 1700)), cat, "Nota.", a));
        Connection loved = connections.save(new Connection(
                works.save(new Work(WorkType.FILM, tag + " amato", "R", 1991)),
                works.save(new Work(WorkType.ARTWORK, tag + " quadro amato", "P", 1701)), cat, "Nota.", a));
        for (int i = 0; i < 3; i++) {
            User fan = users.findByUsername(register(unique("fan")).get("username").asText()).orElseThrow();
            likes.save(Like.builder().user(fan).connection(loved).build());
            comments.save(new Comment(loved, fan, "Bellissimo accostamento."));
        }

        String body = mvc.perform(get("/api/connections/search").param("q", tag).param("sort", "popular"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode content = mapper.readTree(body).get("content");
        assertThat(content.get(0).get("id").asLong()).isEqualTo(loved.getId());
        assertThat(content.get(1).get("id").asLong()).isEqualTo(quiet.getId());
    }
}
