package com.atlante.bootstrap;

import com.atlante.model.*;
import com.atlante.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Simula la vita dell'archivio: un pubblico di utenti demo che mette mi piace e commenta le
 * connessioni. La distribuzione è casuale ma riproducibile (seme fisso per connessione) e
 * sbilanciata come nella realtà: poche connessioni molto amate, una lunga coda tranquilla.
 * Gira una sola volta, dopo l'archivio (si ferma se i mi piace sono già tanti).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class EngagementSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EngagementSeeder.class);

    private final UserRepository users;
    private final ConnectionRepository connections;
    private final LikeRepository likes;
    private final CommentRepository comments;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final com.atlante.service.RoleService roleService;

    @Value("${app.seed.password}")
    private String demoPassword;

    private static final String[][] AUDIENCE = {
            {"sofia", "Sofia Galli"}, {"matteo", "Matteo Russo"}, {"chiara", "Chiara Esposito"},
            {"lorenzo", "Lorenzo Romano"}, {"alice", "Alice Colombo"}, {"tommaso", "Tommaso Ricci"},
            {"beatrice", "Beatrice Marino"}, {"davide", "Davide Greco"}, {"irene", "Irene Bruno"},
            {"pietro", "Pietro Gallo"}, {"elisa", "Elisa Conti"}, {"filippo", "Filippo De Luca"},
            {"anna", "Anna Mancini"}, {"riccardo", "Riccardo Costa"}, {"nina", "Nina Giordano"},
            {"edoardo", "Edoardo Rizzo"}, {"camilla", "Camilla Lombardi"}, {"leonardo", "Leonardo Moretti"},
            {"greta", "Greta Barbieri"}, {"samuele", "Samuele Fontana"}, {"viola", "Viola Santoro"},
            {"jacopo", "Jacopo Caruso"}, {"noemi", "Noemi Mariani"}, {"emanuele", "Emanuele Ferrara"}};

    /** Frasi di commento: {a} e {b} sono le due opere, {c} la categoria. */
    private static final String[] COMMENTS = {
            "Non avevo mai pensato di mettere vicini {a} e {b}. Adesso non riesco più a separarli.",
            "La {c} è davvero il punto in comune: si vede subito, appena li guardi uno accanto all'altro.",
            "Ho rivisto {a} ieri sera pensando a {b}. Cambia il modo in cui guardi certe inquadrature.",
            "Bello l'accostamento, ma secondo me {b} è più inquieto di {a}.",
            "Mi colpisce la stessa solitudine nelle figure. In {b} è ancora più silenziosa.",
            "Le palette si somigliano più di quanto pensassi: guardate i verdi.",
            "Una lettura che non conoscevo. Mi fa venire voglia di andare a vedere {b} dal vero.",
            "Forse è un caso, ma in entrambi la luce arriva sempre da sinistra.",
            "Io ci vedo anche un discorso sul tempo che si ferma.",
            "Accostamento coraggioso. Non sono del tutto convinta, ma mi ha fatto pensare.",
            "La nota critica è bellissima, soprattutto la prima frase.",
            "{a} l'ho visto al cinema anni fa: ricordo esattamente quella sensazione.",
            "Sarebbe bello trovare anche altri film vicini a {b}.",
            "Il punto sulla {c} è perfetto. Aggiungerei la colonna sonora, che fa lo stesso lavoro.",
            "Salvato nella collezione, voglio tornarci con calma.",
            "Guardandoli insieme mi accorgo di dettagli che da soli mi erano sfuggiti.",
            "Questo è il tipo di connessione per cui uso VisuArt.",
            "Mi piace che non si parli di influenza ma di sguardo: è più onesto."};

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) {
        if (likes.count() > 200 || users.findByUsername("elena").isEmpty()) {
            return;
        }
        Instant since = Instant.parse("2026-04-01T10:00:00Z");
        List<User> people = new ArrayList<>();
        for (String[] p : AUDIENCE) {
            people.add(users.findByUsername(p[0]).orElseGet(() -> users.save(User.builder()
                    .username(p[0]).email(p[0] + "@visuart.it").passwordHash(encoder.encode(demoPassword))
                    .displayName(p[1]).roles(roleService.setOf()).createdAt(since).build())));
        }
        for (String u : List.of("elena", "luca", "marta", "giulia", "andrea", "curatore")) {
            users.findByUsername(u).ifPresent(people::add);
        }

        int likeCount = 0, commentCount = 0;
        for (Connection c : connections.findAll()) {
            Random rnd = new Random(c.getId() * 7919L);
            // popolarità sbilanciata: il quadrato di un numero casuale tiene la maggior parte in basso
            double appeal = Math.pow(rnd.nextDouble(), 2);
            List<User> pool = new ArrayList<>(people);
            pool.removeIf(u -> c.getAuthor() != null && u.getId().equals(c.getAuthor().getId()));
            Collections.shuffle(pool, rnd);

            Set<Long> already = new HashSet<>(jdbc.queryForList(
                    "select user_id from connection_likes where connection_id = ?", Long.class, c.getId()));
            int wantLikes = (int) Math.round(2 + appeal * (pool.size() - 2));
            for (User u : pool.subList(0, Math.min(wantLikes, pool.size()))) {
                if (already.add(u.getId())) {
                    likes.save(Like.builder().user(u).connection(c).build());
                    likeCount++;
                }
            }

            int wantComments = (int) Math.round(appeal * 6 + rnd.nextDouble());
            Work a = c.getSource(), b = c.getTarget();
            for (int i = 0; i < wantComments; i++) {
                User u = pool.get((i * 5 + 3) % pool.size());
                String text = COMMENTS[rnd.nextInt(COMMENTS.length)]
                        .replace("{a}", a.getTitle()).replace("{b}", b.getTitle())
                        .replace("{c}", c.getCategory().getName().toLowerCase());
                Comment saved = comments.save(new Comment(c, u, text));
                // commenti distribuiti nelle settimane successive alla pubblicazione
                Instant when = Instant.now().minus(rnd.nextInt(40) + 1, ChronoUnit.DAYS)
                        .minus(rnd.nextInt(24 * 60), ChronoUnit.MINUTES);
                jdbc.update("update comments set created_at = ? where id = ?", java.sql.Timestamp.from(when), saved.getId());
                commentCount++;
            }
        }
        log.info("Interazioni simulate: {} mi piace e {} commenti", likeCount, commentCount);
    }
}
