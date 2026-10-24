package com.atlante.bootstrap;

import com.atlante.client.WikipediaClient;
import com.atlante.client.MetMuseumClient;
import com.atlante.client.TmdbClient;
import com.atlante.model.*;
import com.atlante.repository.CategoryRepository;
import com.atlante.repository.ConnectionRepository;
import com.atlante.repository.UserRepository;
import com.atlante.repository.WorkRepository;
import com.atlante.service.ImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
// Dati demo al primo avvio (database vuoto). In produzione: SEED_DEMO=true e ADMIN_PASSWORD impostata.
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@org.springframework.core.annotation.Order(0)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final CategoryRepository categories;
    private final WorkRepository works;
    private final ConnectionRepository connections;
    private final PasswordEncoder encoder;
    private final TmdbClient tmdb;
    private final MetMuseumClient met;
    private final WikipediaClient wikipedia;
    private final ImportService importService;
    private final com.atlante.service.RoleService roleService;

    @Value("${app.seed.password}")
    private String demoPassword;

    public DataSeeder(UserRepository users, CategoryRepository categories,
                      WorkRepository works, ConnectionRepository connections,
                      PasswordEncoder encoder, TmdbClient tmdb, MetMuseumClient met,
                      WikipediaClient wikipedia, ImportService importService,
                      com.atlante.service.RoleService roleService) {
        this.roleService = roleService;
        this.users = users;
        this.categories = categories;
        this.works = works;
        this.connections = connections;
        this.encoder = encoder;
        this.tmdb = tmdb;
        this.met = met;
        this.wikipedia = wikipedia;
        this.importService = importService;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        User admin = users.save(User.builder()
                .username("admin")
                .email("admin@atlante.local")
                .passwordHash(encoder.encode(demoPassword))
                .displayName("Yharetta")
                .roles(roleService.setOf(Role.ADMIN))
                .build());
        users.save(User.builder()
                .username("curatore")
                .email("curatore@atlante.local")
                .passwordHash(encoder.encode(demoPassword))
                .displayName("Curatore Demo")
                .roles(roleService.setOf(Role.CURATOR))
                .build());

        Category fotografia = categories.save(new Category("Fotografia", "fotografia"));
        Category composizione = categories.save(new Category("Composizione", "composizione"));
        Category tema = categories.save(new Category("Tema", "tema"));
        categories.save(new Category("Montaggio", "montaggio"));
        Category luce = categories.save(new Category("Luce", "luce"));

        Work nostalghia = film("Nostalghia", "Andrej Tarkovskij", 1983,
                "Un poeta russo in Italia, tra memoria e paesaggi immersi nella nebbia.",
                "#4a5548,#8a927e,#c9c4ae,#2e3230");
        Work stalker = film("Stalker", "Andrej Tarkovskij", 1979,
                "Tre uomini attraversano la Zona verso la stanza dei desideri.",
                "#5a5a48,#8a7e5a,#3d4436,#b0a58a");
        Work mirror = film("Lo specchio", "Andrej Tarkovskij", 1975,
                "Frammenti di memoria familiare e storia russa.",
                "#6e7a6a,#b0a890,#4a4438,#d6cdb8");
        Work barry = film("Barry Lyndon", "Stanley Kubrick", 1975,
                "Ascesa e caduta di un avventuriero irlandese nel Settecento.",
                "#8c7040,#d9c48a,#3a4a2a,#1e1a14");
        Work caravaggioVocazione = artwork("Vocazione di San Matteo", "Caravaggio", 1600,
                "La luce taglia la stanza e indica Matteo tra i gabellieri.",
                "#2b1d12,#8c5a2a,#d9a85a,#4a3520");
        Work vermeerLattaia = artwork("La lattaia", "Johannes Vermeer", 1660,
                "Luce da finestra su un gesto quotidiano, silenzio domestico.",
                "#4a6a8a,#d9c46a,#8c3a2a,#e8e2d0");
        Work friedrichViandante = artwork("Viandante sul mare di nebbia", "Caspar David Friedrich", 1818,
                "Figura di spalle davanti all'immensità della natura.",
                "#5a7c8c,#8ea3ad,#d8ceb8,#2e3a42");
        Work hopperNighthawks = artwork("Nighthawks", "Edward Hopper", 1942,
                "Un diner notturno, solitudine urbana sotto luce artificiale.",
                "#2b3a42,#5a6a7a,#d9c48a,#1a5c4a");

        connect(nostalghia, caravaggioVocazione, luce, admin,
                "La luce radente che entra dalle aperture negli interni di Nostalghia isola le figure "
                        + "dal fondo scuro come il fascio che attraversa la Vocazione: la luce come "
                        + "chiamata, non come illuminazione naturalistica.");
        connect(barry, vermeerLattaia, fotografia, admin,
                "Gli interni a lume di candela di Barry Lyndon riprendono la luce laterale da finestra "
                        + "di Vermeer: composizione pittorica, tempo sospeso, materia luminosa.");
        connect(stalker, friedrichViandante, tema, admin,
                "La Zona è il sublime romantico di Friedrich: l'uomo minuscolo davanti a una natura "
                        + "che non gli appartiene, visto quasi sempre di spalle.");
        connect(mirror, friedrichViandante, composizione, admin,
                "Le figure di spalle davanti al paesaggio nello Specchio ereditano la Rückenfigur "
                        + "romantica: lo spettatore entra nel quadro attraverso il corpo di chi guarda.");
        connect(nostalghia, hopperNighthawks, composizione, admin,
                "Gli interni spogli e le finestre come quinte di Nostalghia condividono con Hopper "
                        + "la geometria della solitudine: spazi vuoti che premono sulle figure.");

        Work bladeRunner = film("Blade Runner 2049", "Denis Villeneuve", 2017,
                "Un blade runner scopre un segreto che può cambiare la società.",
                "#1b2a41,#3a86a8,#d4537e,#e8a33d");
        Work inTheMood = film("In the Mood for Love", "Wong Kar-wai", 2000,
                "Due vicini di casa e il tradimento dei rispettivi coniugi.",
                "#7a3b4a,#993556,#c98b3a,#2e1e22");
        Work treeOfLife = film("The Tree of Life", "Terrence Malick", 2011,
                "Memoria familiare e origini del cosmo intrecciate.",
                "#3f6b5a,#cfe0d0,#8fb3c9,#e8d8b0");
        Work parasite = film("Parasite", "Bong Joon-ho", 2019,
                "Una famiglia povera si infiltra in una casa ricca.",
                "#3a3a42,#7a8a99,#e8c78a,#1e2a1e");
        Work odissea = film("2001: Odissea nello spazio", "Stanley Kubrick", 1968,
                "Dall'alba dell'uomo a Giove, oltre l'infinito.",
                "#17171a,#e0e0e0,#c1440e,#7a7a80");
        Work amelie = film("Il favoloso mondo di Amélie", "Jean-Pierre Jeunet", 2001,
                "Una cameriera parigina ripara in segreto le vite altrui.",
                "#c98b3a,#7a8a3a,#5c2b2e,#e8c78a");
        Work donnaLegge = artwork("Donna che legge una lettera", "Johannes Vermeer", 1663,
                "Una donna assorta nella lettura, luce da sinistra.",
                "#6b5a3a,#8c7040,#d9d2b8,#3a4a5a");
        Work americanGothic = artwork("American Gothic", "Grant Wood", 1930,
                "Un contadino e la figlia davanti alla casa gotica.",
                "#6b5030,#d9b878,#e0e0e0,#2e2a22");
        Work quadratoNero = artwork("Quadrato nero", "Kazimir Malevič", 1915,
                "L'azzeramento suprematista della pittura.",
                "#101013,#f0ede6,#3a3a3e,#c9c4bc");
        Work balMoulin = artwork("Bal du moulin de la Galette", "Pierre-Auguste Renoir", 1876,
                "Domenica pomeriggio danzante a Montmartre.",
                "#d9b878,#7a8a99,#4a5568,#e8dcc8");

        connect(bladeRunner, hopperNighthawks, fotografia, admin,
                "La stessa solitudine urbana illuminata da neon e vetrine notturne attraversa le due "
                        + "opere: la luce non riscalda, espone. Villeneuve cita Hopper senza dichiararlo; "
                        + "la composizione — figure sospese dietro il vetro — è la stessa.");
        connect(inTheMood, donnaLegge, composizione, admin,
                "Corridoi e soglie inquadrano i personaggi come Vermeer inquadrava le sue figure: "
                        + "cornici dentro la cornice, intimità osservata da una distanza discreta.");
        connect(treeOfLife, friedrichViandante, tema, admin,
                "Il sublime romantico ritorna nell'uomo minuscolo davanti al cosmo: Malick riprende "
                        + "da Friedrich la sproporzione tra figura e creato come domanda spirituale.");
        connect(parasite, americanGothic, tema, admin,
                "Due ritratti di classe sociale, uno ironico e uno feroce: la frontalità rigida di "
                        + "Wood diventa in Bong la maschera che le famiglie indossano l'una per l'altra.");
        connect(odissea, quadratoNero, composizione, admin,
                "Il monolite è l'astrazione geometrica pura portata al cinema: come il Quadrato di "
                        + "Malevič, un oggetto che rifiuta l'interpretazione e la genera all'infinito.");
        connect(amelie, balMoulin, fotografia, admin,
                "La stessa palette calda di ambra e verde bottiglia scalda la Parigi di Jeunet come "
                        + "quella di Renoir: la città come festa popolare, la luce come affetto.");

        enrichImages();

        System.out.println("Seed completato: login demo admin@atlante.local");
    }

    /**
     * Prova a scaricare un'immagine per ogni opera del seed: locandine da TMDB
     * per i film, riproduzioni da Art Institute of Chicago (poi Met) per i quadri.
     * Se la rete o la ricerca falliscono, l'app funziona comunque senza immagini.
     */
    private static String commons(String fileName) {
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + fileName + "?width=600";
    }

    private void enrichImages() {
        // URL diretti (Wikimedia Commons, pubblico dominio) per i quadri del seed:
        // molti non sono nelle collezioni AIC/Met, quindi la ricerca museale fallirebbe.
        java.util.Map<String, String> artworkUrls = new java.util.HashMap<>();
        artworkUrls.put("Vocazione di San Matteo",
                commons("The_Calling_of_Saint_Matthew-Caravaggo_(1599-1600).jpg"));
        artworkUrls.put("La lattaia",
                commons("Johannes_Vermeer_-_Het_melkmeisje_-_Google_Art_Project.jpg"));
        artworkUrls.put("Viandante sul mare di nebbia",
                commons("Caspar_David_Friedrich_-_Wanderer_above_the_sea_of_fog.jpg"));
        artworkUrls.put("Nighthawks",
                commons("Nighthawks_by_Edward_Hopper_1942.jpg"));
        artworkUrls.put("Donna che legge una lettera",
                commons("Vermeer,_Johannes_-_Woman_reading_a_letter_-_c._1662-1663.jpg"));
        artworkUrls.put("American Gothic",
                commons("Grant_Wood_-_American_Gothic_-_Google_Art_Project.jpg"));
        artworkUrls.put("Quadrato nero",
                commons("Kazimir_Malevich,_1915,_Black_Suprematic_Square,_oil_on_linen_canvas,_79.5_x_79.5_cm,_Tretyakov_Gallery,_Moscow.jpg"));
        artworkUrls.put("Bal du moulin de la Galette",
                commons("Pierre-Auguste_Renoir,_Le_Moulin_de_la_Galette.jpg"));

        java.util.Map<String, String> englishTitles = java.util.Map.of(
                "Vocazione di San Matteo", "The Calling of Saint Matthew",
                "La lattaia", "The Milkmaid",
                "Viandante sul mare di nebbia", "Wanderer above the Sea of Fog",
                "Donna che legge una lettera", "Woman Reading a Letter",
                "Quadrato nero", "Black Square",
                "Bal du moulin de la Galette", "Ball at the Moulin de la Galette");

        for (Work w : works.findAll()) {
            if (w.getImageUrl() != null) {
                continue;
            }
            try {
                String url = null;
                if (w.getType() == WorkType.FILM && tmdb.isConfigured()) {
                    // il primo risultato per titolo può essere un altro film ("Lo specchio" →
                    // "Alice attraverso lo specchio"): si preferisce quello dello stesso anno
                    var found = tmdb.search(w.getTitle());
                    url = found.stream()
                            .filter(r -> w.getYear() != null && w.getYear().equals(r.year()))
                            .findFirst()
                            .or(() -> found.stream().findFirst())
                            .map(r -> r.imageUrl()).orElse(null);
                } else if (w.getType() == WorkType.ARTWORK) {
                    url = artworkUrls.get(w.getTitle());
                    if (url == null) {
                        String query = englishTitles.getOrDefault(w.getTitle(), w.getTitle());
                        url = wikipedia.findImageUrl(query, w.getCreator())
                                .orElseGet(() -> met.search(query).stream().findFirst()
                                        .map(r -> r.imageUrl()).orElse(null));
                    }
                }
                if (url != null) {
                    importService.attachImage(w.getId(), url);
                    System.out.println("Immagine agganciata: " + w.getTitle());
                }
            } catch (Exception e) {
                System.out.println("Nessuna immagine per '" + w.getTitle() + "': " + e.getMessage());
            }
        }
    }

    private Work film(String title, String director, int year, String desc, String palette) {
        Work w = new Work(WorkType.FILM, title, director, year);
        w.setDescription(desc);
        w.setPalette(palette);
        return works.save(w);
    }

    private Work artwork(String title, String artist, int year, String desc, String palette) {
        Work w = new Work(WorkType.ARTWORK, title, artist, year);
        w.setDescription(desc);
        w.setPalette(palette);
        return works.save(w);
    }

    private void connect(Work source, Work target, Category cat, User author, String note) {
        connections.save(new Connection(source, target, cat, note, author));
    }
}
