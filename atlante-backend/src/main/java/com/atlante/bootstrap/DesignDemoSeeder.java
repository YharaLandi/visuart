package com.atlante.bootstrap;

import com.atlante.model.*;
import com.atlante.repository.*;
import com.atlante.service.PaletteExtractor;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Contenuti dimostrativi allineati al design VisuArt: le persone, le opere, le tre
 * connessioni in evidenza, i dialoghi e una chat d'esempio. Gira dopo il seeder base e
 * una sola volta (se l'utente "elena" esiste già non fa nulla), senza toccare altri dati.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class DesignDemoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DesignDemoSeeder.class);

    private final UserRepository users;
    private final CategoryRepository categories;
    private final WorkRepository works;
    private final ConnectionRepository connections;
    private final CommentRepository comments;
    private final LikeRepository likes;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    @Value("${app.seed.password}")
    private String demoPassword;

    @Value("${app.images.storage-dir}")
    private String storageDir;

    /** Palette ricavate dalle card del design (l'estrazione automatica privilegia le zone scure). */
    private static final java.util.Map<String, String> PALETTES = java.util.Map.of(
            "Barry Lyndon", "#3a2f22,#6e5236,#a08550,#c9b07a,#e3d3b0,#707680",
            "La lattaia", "#e3d3a5,#efe3c4,#b59a45,#3a4f7a,#7d8a64,#6b5233",
            "Paris, Texas", "#1e3b33,#466b4b,#8fae80,#c7442f,#e2a24a,#2f2a33",
            "Gas", "#23302a,#4b6448,#b24a33,#c47f45,#e1c99a,#5a4a33",
            "Stalker", "#2e3a2c,#5b6b4c,#8c9378,#4a5a62,#b9b49a,#1f2420",
            "L’isola dei morti", "#1f2a2a,#3e4f4c,#6b6f5f,#9a9277,#c8c2a8,#14191a");

    @Override
    @Transactional
    public void run(String... args) {
        if (users.findByUsername("elena").isPresent()) {
            syncPalettes();
            return;
        }
        Instant march = Instant.parse("2026-03-10T10:00:00Z");
        User elena = person("elena", "elena.rossi@visuart.it", "Elena Rossi", Role.CURATOR,
                "Cerco le immagini che continuano oltre la loro cornice.", march);
        User luca = person("luca", "luca.bianchi@visuart.it", "Luca Bianchi", Role.CURATOR, null, march);
        User marta = person("marta", "marta.conti@visuart.it", "Marta Conti", Role.USER, null, march);
        person("giulia", "giulia.ferri@visuart.it", "Giulia Ferri", Role.USER, null, march);
        person("andrea", "andrea.moretti@visuart.it", "Andrea Moretti", Role.USER, null, march);

        Category luce = category("Luce", "luce");
        Category composizione = category("Composizione", "composizione");
        Category colore = category("Colore", "colore");
        Category tema = category("Tema", "tema");

        Work mood = work(WorkType.FILM, "In the Mood for Love", "Wong Kar-wai", 2000, "design-in-the-mood.jpg",
                "#243d38,#668375,#a07e50,#ad3e31,#d7b88e,#272725");
        Work nighthawks = work(WorkType.ARTWORK, "Nighthawks", "Edward Hopper", 1942, "design-nighthawks.jpg",
                "#142c29,#3d5550,#b8904d,#a34c34,#e7c98f,#292f28");
        Work barry = work(WorkType.FILM, "Barry Lyndon", "Stanley Kubrick", 1975, "design-barry-lyndon.jpg", null);
        Work lattaia = work(WorkType.ARTWORK, "La lattaia", "Johannes Vermeer", 1660, "design-lattaia.jpg", null);
        Work paris = work(WorkType.FILM, "Paris, Texas", "Wim Wenders", 1984, "design-paris-texas.jpg", null);
        Work gas = work(WorkType.ARTWORK, "Gas", "Edward Hopper", 1940, "design-gas.jpg", null);
        Work stalker = work(WorkType.FILM, "Stalker", "Andrej Tarkovskij", 1979, "design-stalker.jpg", null);
        Work isola = work(WorkType.ARTWORK, "L’isola dei morti", "Arnold Böcklin", 1880, "design-isola.jpg", null);

        Connection c042 = connect(mood, nighthawks, luce, elena, """
                La luce non illumina soltanto: separa chi guarda da chi vorrebbe essere visto. \
                Nel corridoio di Wong Kar-wai e nel diner di Hopper, la notte si raccoglie attorno \
                a una luce artificiale. Le figure sono vicine, eppure qualcosa le trattiene: un vetro, \
                una soglia, un gesto rimasto a metà.

                Mi interessa questa intimità senza approdo. Il verde tiene a distanza il calore del \
                rosso e dell’ambra; l’inquadratura diventa una stanza dalla quale non sappiamo uscire. \
                Non vedo una citazione, ma una stessa domanda: quanto può essere abitata la solitudine?""", 12840);
        Connection c018 = connect(barry, lattaia, composizione, elena, """
                La finestra diventa una regia silenziosa: i gesti quotidiani acquistano il peso di un \
                ritratto. Kubrick illumina le stanze come Vermeer, con una sola sorgente laterale che \
                scivola sui volumi e lascia il resto nella penombra.

                In entrambi il tempo rallenta fino a fermarsi. La quotidianità smette di essere sfondo \
                e diventa il soggetto.""", 9216);
        Connection c031 = connect(paris, gas, colore, elena, """
                Il rosso interrompe il paesaggio. Una sosta diventa il luogo fragile di un possibile \
                ritorno. Wenders e Hopper fermano la strada americana in un distributore al crepuscolo: \
                un’isola di luce artificiale tra il buio che avanza e un cielo che si spegne.

                Sono luoghi di passaggio, non di arrivo. Le figure aspettano qualcosa che forse non \
                arriverà.""", 8704);
        connect(stalker, isola, tema, elena, """
                La natura si riprende ciò che l’uomo ha lasciato. La Zona di Tarkovskij e l’isola di \
                Böcklin sono luoghi in cui si entra in silenzio, quasi in barca, verso qualcosa che non \
                si può nominare.""", 3120);

        comment(c042, luca, "Mi colpisce il vetro: in entrambi i casi rende la vicinanza una forma di distanza.");
        comment(c042, marta, "Forse è proprio l’assenza di un incontro a rendere così presente la luce. "
                + "Un accostamento che invita a rivedere il film.");
        for (Connection c : List.of(c042, c018, c031)) {
            for (String u : List.of("luca", "marta", "giulia", "andrea", "admin", "curatore")) {
                users.findByUsername(u).ifPresent(user -> likes.save(Like.builder().user(user).connection(c).build()));
            }
        }

        // chat d'esempio: Luca scrive a Elena a proposito della connessione #042
        Conversation conv = conversations.save(Conversation.between(elena, luca));
        message(conv, luca, elena, "", c042.getId());
        message(conv, luca, elena, "Ho riletto la tua nota su Hopper. Secondo te il vetro protegge le figure o le isola?", null);
        message(conv, elena, luca, "Mi sembra faccia entrambe le cose. L’intimità nasce proprio da quella distanza.", null);
        message(conv, luca, elena, "Mi piace questa idea della soglia. Nel film, anche i corridoi funzionano così.", null);
        message(conv, elena, luca, "Sì, come se la stanza non fosse mai davvero privata.", null);
        messages.markRead(conv.getId(), "elena", MessageStatus.READ);
        messages.markRead(conv.getId(), "luca", MessageStatus.READ);
        Conversation withMarta = conversations.save(Conversation.between(elena, marta));
        message(withMarta, marta, elena, "Hai visto l’accostamento su Vermeer?", null);

        log.info("Contenuti demo del design caricati (accesso: elena / password demo)");
    }

    private User person(String username, String email, String name, Role role, String bio, Instant since) {
        return users.findByUsername(username).orElseGet(() -> users.save(User.builder()
                .username(username).email(email).passwordHash(encoder.encode(demoPassword))
                .displayName(name).role(role).bio(bio).createdAt(since).build()));
    }

    private Category category(String name, String slug) {
        return categories.findBySlug(slug).orElseGet(() -> categories.save(new Category(name, slug)));
    }

    /** Usa l'opera se esiste già (stesso titolo e tipo), altrimenti la crea; immagine e palette dal design. */
    private Work work(WorkType type, String title, String creator, Integer year, String image, String palette) {
        Optional<Work> existing = works.findAll().stream()
                .filter(w -> w.getType() == type && w.getTitle().equalsIgnoreCase(title))
                .findFirst();
        Work w = existing.orElseGet(() -> new Work(type, title, creator, year));
        Path file = Path.of(storageDir, image);
        if (Files.exists(file)) {
            w.setImageUrl("/images/" + image);
            w.setPalette(palette != null ? palette : PALETTES.getOrDefault(title, PaletteExtractor.extract(file, 6)));
        }
        return works.save(w);
    }

    /** Riallinea le palette delle opere del design anche su database già popolati. */
    private void syncPalettes() {
        works.findAll().stream()
                .filter(w -> w.getImageUrl() != null && w.getImageUrl().startsWith("/images/design-"))
                .filter(w -> PALETTES.containsKey(w.getTitle()) && !PALETTES.get(w.getTitle()).equals(w.getPalette()))
                .forEach(w -> {
                    w.setPalette(PALETTES.get(w.getTitle()));
                    works.save(w);
                });
    }

    private Connection connect(Work source, Work target, Category category, User author, String note, long views) {
        Connection c = connections.save(new Connection(source, target, category, note, author));
        jdbc.update("update connections set view_count = ? where id = ?", views, c.getId());
        return c;
    }

    private void comment(Connection c, User author, String text) {
        comments.save(new Comment(c, author, text));
    }

    private void message(Conversation conv, User from, User to, String text, Long connectionId) {
        messages.save(new Message(conv, from, to, text, connectionId));
    }
}
