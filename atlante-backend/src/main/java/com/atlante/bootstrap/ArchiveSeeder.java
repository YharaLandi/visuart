package com.atlante.bootstrap;

import com.atlante.client.TmdbClient;
import com.atlante.client.WikipediaClient;
import com.atlante.model.*;
import com.atlante.repository.*;
import com.atlante.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Popola l'archivio fino a oltre cinquanta connessioni: accostamenti film–opera, film–film e
 * opera–opera firmati da autori diversi, con viste, mi piace e qualche dialogo. Gira dopo i
 * contenuti del design e una sola volta (si ferma se l'archivio ha già cinquanta connessioni).
 * Le immagini arrivano da TMDB (poster dell'anno giusto) e da Wikipedia (quadri); la palette
 * viene estratta dall'immagine scaricata.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE - 5)
@RequiredArgsConstructor
public class ArchiveSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ArchiveSeeder.class);
    private static final int TARGET = 50;

    private final UserRepository users;
    private final CategoryRepository categories;
    private final WorkRepository works;
    private final ConnectionRepository connections;
    private final CommentRepository comments;
    private final LikeRepository likes;
    private final ImportService importService;
    private final TmdbClient tmdb;
    private final WikipediaClient wikipedia;
    private final JdbcTemplate jdbc;

    @org.springframework.beans.factory.annotation.Value("${app.images.storage-dir}")
    private String storageDir;

    /** Titoli inglesi per trovare i quadri su Wikipedia. */
    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("La persistenza della memoria", "The Persistence of Memory"),
            Map.entry("Notte stellata", "The Starry Night"),
            Map.entry("L’urlo", "The Scream"),
            Map.entry("La grande onda di Kanagawa", "The Great Wave off Kanagawa"),
            Map.entry("Ofelia", "Ophelia (painting)"),
            Map.entry("Il bacio", "The Kiss (Klimt)"),
            Map.entry("Las Meninas", "Las Meninas"),
            Map.entry("La zattera della Medusa", "The Raft of the Medusa"),
            Map.entry("Impressione, levar del sole", "Impression, Sunrise"),
            Map.entry("Christina’s World", "Christina's World"),
            Map.entry("Il figlio dell’uomo", "The Son of Man"),
            Map.entry("Morning Sun", "Morning Sun (Hopper)"),
            Map.entry("Automat", "Automat (Hopper)"),
            Map.entry("Saturno che divora i suoi figli", "Saturn Devouring His Son"),
            Map.entry("La morte di Marat", "The Death of Marat"),
            Map.entry("Il giardino delle delizie", "The Garden of Earthly Delights"),
            Map.entry("Composizione VIII", "Composition VIII"),
            Map.entry("Ninfee", "Water Lilies (Monet series)"),
            Map.entry("L’incubo", "The Nightmare (Fuseli)"));

    @Override
    public void run(String... args) {
        if (users.findByUsername("elena").isEmpty()) {
            return;
        }
        if (connections.count() >= TARGET) {
            // archivio già popolato: si recuperano solo le immagini che non erano arrivate
            attachImages();
            return;
        }
        User elena = user("elena"), luca = user("luca"), marta = user("marta"),
                giulia = user("giulia"), andrea = user("andrea"), curatore = user("curatore");
        Category luce = cat("luce"), composizione = cat("composizione"), colore = cat("colore"),
                tema = cat("tema"), fotografia = cat("fotografia"), montaggio = cat("montaggio");

        // film
        Work vertigo = film("La donna che visse due volte", "Alfred Hitchcock", 1958);
        Work desertoRosso = film("Il deserto rosso", "Michelangelo Antonioni", 1964);
        Work shining = film("Shining", "Stanley Kubrick", 1980);
        Work giorniCielo = film("I giorni del cielo", "Terrence Malick", 1978);
        Work melancholia = film("Melancholia", "Lars von Trier", 2011);
        Work settimoSigillo = film("Il settimo sigillo", "Ingmar Bergman", 1957);
        Work persona = film("Persona", "Ingmar Bergman", 1966);
        Work moonlight = film("Moonlight", "Barry Jenkins", 2016);
        Work laLaLand = film("La La Land", "Damien Chazelle", 2016);
        Work her = film("Lei", "Spike Jonze", 2013);
        Work lost = film("Lost in Translation", "Sofia Coppola", 2003);
        Work carol = film("Carol", "Todd Haynes", 2015);
        Work drive = film("Drive", "Nicolas Winding Refn", 2011);
        Work budapest = film("Grand Budapest Hotel", "Wes Anderson", 2014);
        Work lighthouse = film("The Lighthouse", "Robert Eggers", 2019);
        Work ritratto = film("Ritratto della giovane in fiamme", "Céline Sciamma", 2019);
        Work arancia = film("Arancia meccanica", "Stanley Kubrick", 1971);
        Work suspiria = film("Suspiria", "Dario Argento", 1977);
        Work dolceVita = film("La dolce vita", "Federico Fellini", 1960);
        Work avventura = film("L'avventura", "Michelangelo Antonioni", 1960);
        Work rublev = film("Andrej Rublëv", "Andrej Tarkovskij", 1966);
        Work roma = film("Roma", "Alfonso Cuarón", 2018);
        Work otto = film("8½", "Federico Fellini", 1963);
        Work gattopardo = film("Il Gattopardo", "Luchino Visconti", 1963);
        Work stalker = existing(WorkType.FILM, "Stalker");
        Work mood = existing(WorkType.FILM, "In the Mood for Love");
        Work odissea = existing(WorkType.FILM, "2001: Odissea nello spazio");

        // opere d'arte
        Work memoria = art("La persistenza della memoria", "Salvador Dalí", 1931);
        Work notte = art("Notte stellata", "Vincent van Gogh", 1889);
        Work urlo = art("L’urlo", "Edvard Munch", 1893);
        Work onda = art("La grande onda di Kanagawa", "Katsushika Hokusai", 1831);
        Work ofelia = art("Ofelia", "John Everett Millais", 1852);
        Work bacio = art("Il bacio", "Gustav Klimt", 1908);
        Work meninas = art("Las Meninas", "Diego Velázquez", 1656);
        Work medusa = art("La zattera della Medusa", "Théodore Géricault", 1819);
        Work impressione = art("Impressione, levar del sole", "Claude Monet", 1872);
        Work christina = art("Christina’s World", "Andrew Wyeth", 1948);
        Work figlioUomo = art("Il figlio dell’uomo", "René Magritte", 1964);
        Work morningSun = art("Morning Sun", "Edward Hopper", 1952);
        Work automat = art("Automat", "Edward Hopper", 1927);
        Work saturno = art("Saturno che divora i suoi figli", "Francisco Goya", 1823);
        Work marat = art("La morte di Marat", "Jacques-Louis David", 1793);
        Work delizie = art("Il giardino delle delizie", "Hieronymus Bosch", 1500);
        Work kandinsky = art("Composizione VIII", "Vasilij Kandinskij", 1923);
        Work ninfee = art("Ninfee", "Claude Monet", 1916);
        Work nighthawks = existing(WorkType.ARTWORK, "Nighthawks");
        Work gas = existing(WorkType.ARTWORK, "Gas");
        Work isola = existing(WorkType.ARTWORK, "L’isola dei morti");
        Work matteo = existing(WorkType.ARTWORK, "Vocazione di San Matteo");
        Work galette = existing(WorkType.ARTWORK, "Bal du moulin de la Galette");
        Work turbante = existing(WorkType.ARTWORK, "Ragazza col turbante");

        Connection c1 = connect(vertigo, memoria, tema, luca, 4120, """
                Il tempo che si scioglie. Scottie insegue una donna che è già un ricordo, e gli orologi \
                di Dalí colano come la sua ossessione: in entrambi il passato non passa, si deforma.

                La spirale dei titoli di testa e il paesaggio immobile di Port Lligat raccontano la stessa \
                vertigine, quella di chi guarda troppo a lungo la stessa immagine.""");
        connect(desertoRosso, kandinsky, colore, elena, 3380, """
                Il colore smette di descrivere e comincia a sentire. Antonioni dipinge di grigio la frutta \
                e di rosso le pareti come Kandinskij affida a cerchi e linee lo stato d'animo, non la forma.

                Giuliana attraversa una fabbrica che è già una composizione astratta: il disagio è cromatico.""");
        Connection c3 = connect(shining, meninas, composizione, andrea, 5210, """
                Chi guarda chi. I corridoi simmetrici dell'Overlook sono uno specchio come quello sul fondo \
                di Las Meninas: la scena si apre su uno spazio che contiene anche lo spettatore.

                Kubrick, come Velázquez, ci mette dentro il quadro e poi ci chiude la porta alle spalle.""");
        connect(giorniCielo, christina, luce, giulia, 2970, """
                L'ora magica e una casa lontana. Malick gira quasi solo al tramonto, Wyeth dipinge l'erba \
                dorata che separa Christina dalla fattoria: la luce bassa rende ogni distanza struggente.

                In entrambi la pianura è un desiderio, non un luogo.""");
        Connection c5 = connect(melancholia, ofelia, tema, elena, 4460, """
                Una sposa nell'acqua. Justine si lascia galleggiare nel ruscello con l'abito bianco, ed è \
                Ofelia di Millais citata alla lettera: la bellezza diventa l'attesa della fine.

                Von Trier prende la malinconia preraffaellita e la porta su scala cosmica.""");
        connect(settimoSigillo, delizie, tema, curatore, 2540, """
                La danza macabra sul crinale e i dannati di Bosch appartengono allo stesso Medioevo \
                immaginato: un mondo affollato di simboli dove la morte gioca a scacchi con i vivi.

                Bergman sceglie la sintesi, Bosch l'eccesso, ma la domanda sul senso resta identica.""");
        connect(persona, figlioUomo, composizione, luca, 3010, """
                Il volto nascosto. Bergman sovrappone due facce fino a farne una, Magritte copre la sua con \
                una mela: in entrambi l'identità è un'immagine che si può spostare, coprire, scambiare.""");
        connect(moonlight, notte, colore, marta, 3890, """
                Il blu che accoglie. Jenkins illumina la pelle dei suoi personaggi di blu e di viola, come Van \
                Gogh fa vibrare il cielo sopra il villaggio: il colore della notte diventa tenerezza.

                Non è il blu della tristezza, è quello della protezione.""");
        connect(laLaLand, impressione, colore, giulia, 4730, """
                Il cielo viola sopra Los Angeles e il sole arancio sul porto di Le Havre sono la stessa \
                impressione: un momento che dura pochi minuti e che vale tutto il resto.

                Chazelle, come Monet, dipinge la luce prima delle persone.""");
        connect(her, morningSun, luce, elena, 3560, """
                Una donna seduta nel sole, da sola, in una stanza che guarda la città. Hopper e Jonze \
                raccontano la stessa solitudine luminosa: calda, pulita, e proprio per questo inquieta.""");
        connect(lost, automat, tema, andrea, 3340, """
                La notte in un posto che non è casa. La ragazza di Hopper davanti alla tazza e Charlotte \
                alla finestra dell'hotel di Tokyo condividono lo stesso sguardo basso: stanchezza e attesa.""");
        connect(carol, nighthawks, fotografia, marta, 2890, """
                Vetri bagnati e luce al neon. Haynes filma Carol quasi sempre attraverso un finestrino o \
                una vetrina, come Hopper ci tiene fuori dal diner: amare significa guardare da lontano.""");
        connect(drive, gas, luce, luca, 2610, """
                Una stazione di servizio di notte. Il pilota di Refn e il benzinaio di Hopper abitano la \
                stessa America sospesa, dove la luce artificiale è l'unica cosa che resta accesa.""");
        connect(budapest, bacio, colore, giulia, 3150, """
                Oro, rosa e geometria. Anderson costruisce i suoi interni come Klimt decora i mantelli: \
                motivi ripetuti, simmetrie, una ricchezza ornamentale che nasconde la malinconia di un mondo finito.""");
        connect(lighthouse, saturno, tema, curatore, 2280, """
                Il nero che divora. Il faro di Eggers e il dio di Goya condividono lo stesso buio pittorico: \
                figure deformate dalla follia, chiuse in uno spazio da cui la luce non esce.""");
        Connection c16 = connect(ritratto, turbante, luce, elena, 4380, """
                Uno sguardo che si volta. Héloïse e la ragazza di Vermeer guardano chi le dipinge con la \
                stessa intensità: Sciamma racconta il ritratto dal punto di vista di chi viene ritratta.

                La luce laterale scolpisce i volti, e il fondo scuro lascia spazio solo al guardarsi.""");
        connect(arancia, urlo, montaggio, andrea, 3470, """
                Il grido fuori campo. Kubrick monta l'ultraviolenza al ritmo della Nona, Munch deforma il \
                paesaggio attorno a una bocca aperta: in entrambi l'angoscia diventa un'onda che piega lo spazio.""");
        connect(suspiria, delizie, colore, marta, 2730, """
                Rossi, blu e verdi impossibili. Argento illumina l'accademia di danza come un trittico di \
                Bosch: un paradiso artificiale che si rovescia in inferno a ogni stanza.""");
        connect(dolceVita, galette, montaggio, luca, 3920, """
                La festa come spettacolo. Fellini e Renoir fanno girare la folla senza un centro: si passa \
                da un volto all'altro come in un ballo, e la gioia ha sempre un fondo di stanchezza.""");
        connect(avventura, christina, composizione, giulia, 2440, """
                Una figura piccola in un paesaggio enorme. Antonioni perde una donna su un'isola, Wyeth \
                lascia Christina nell'erba: lo spazio vuoto pesa più dei personaggi.""");
        connect(rublev, matteo, luce, curatore, 2860, """
                La luce che chiama. Il raggio di Caravaggio sopra il tavolo dei pubblicani e la campana di \
                Rublëv che finalmente suona: la grazia arriva in un luogo umile, senza preavviso.""");
        connect(roma, onda, tema, elena, 3610, """
                Il mare che sale. La scena sulla spiaggia di Cuarón è la grande onda di Hokusai vista da \
                dentro: la natura sovrasta, e chi resta in piedi lo fa tenendosi per mano.""");
        connect(otto, delizie, montaggio, andrea, 2510, """
                Un affresco della mente. Fellini monta ricordi, sogni e desideri come Bosch accumula \
                figure nei pannelli: non c'è un racconto lineare, c'è un mondo interiore da percorrere.""");
        Connection c24 = connect(gattopardo, meninas, fotografia, luca, 3980, """
                Il ballo e la corte. Visconti filma il salone di palazzo Ponteleone con la profondità di \
                Velázquez: decine di figure, specchi, porte aperte, e al centro un'epoca che sta finendo.""");

        // film con film
        connect(stalker, rublev, tema, marta, 2790, """
                Due film di Tarkovskij che si parlano. L'acqua, il fango, il fuoco: la Zona e la Russia di \
                Rublëv sono paesaggi dove la fede si mette alla prova attraversando la materia.""");
        connect(vertigo, mood, colore, giulia, 3230, """
                Il verde dell'ossessione. Madeleine emerge dal neon verde dell'albergo come Su Li-zhen nei \
                corridoi di Wong Kar-wai: due amori che esistono soprattutto nello sguardo di chi li insegue.""");
        connect(lost, her, tema, elena, 3700, """
                Due città, due solitudini gentili. Coppola e Jonze, quasi negli stessi anni, raccontano \
                l'intimità con qualcuno che non resterà: Tokyo di notte e una Los Angeles dai colori pastello.""");
        connect(shining, odissea, composizione, curatore, 3120, """
                La simmetria come minaccia. Kubrick mette al centro dell'inquadratura il corridoio \
                dell'Overlook e il monolite: la perfezione geometrica è il segnale che qualcosa ci osserva.""");
        connect(dolceVita, otto, montaggio, andrea, 2650, """
                Fellini prima e dopo. Marcello attraversa Roma di notte, Guido attraversa la sua memoria: \
                lo stesso corteo di volti, ma in 8½ la festa si è spostata dentro la testa del regista.""");
        connect(desertoRosso, avventura, fotografia, luca, 2390, """
                Antonioni in bianco e nero e a colori. Lo stesso vuoto attorno alle figure, la stessa \
                difficoltà di parlarsi: nel Deserto rosso il colore rende visibile quello che L'avventura taceva.""");

        // opera con opera
        connect(nighthawks, automat, luce, marta, 3060, """
                Hopper due volte. La donna dell'Automat siede sola dove, quindici anni dopo, il diner \
                raccoglierà altri solitari: la luce elettrica è il loro unico interlocutore.""");
        connect(onda, notte, composizione, giulia, 3510, """
                Il vortice. Van Gogh conosceva le stampe giapponesi, e la sua notte si arriccia come \
                l'onda di Hokusai: due cieli e un mare che si avvolgono su sé stessi.""");
        connect(ofelia, isola, tema, elena, 2470, """
                Acqua ferma, fiori e un viaggio senza ritorno. Millais e Böcklin dipingono la morte come un \
                approdo silenzioso, circondato da una natura che sembra trattenere il respiro.""");
        connect(marat, matteo, luce, curatore, 2810, """
                Il buio sopra, la luce sul corpo. David impara da Caravaggio a lasciare metà quadro nel \
                nero: Marat e Matteo sono colpiti dallo stesso raggio, uno nella morte e l'altro nella chiamata.""");
        connect(impressione, ninfee, colore, andrea, 2330, """
                Monet all'inizio e alla fine. Dal porto di Le Havre allo stagno di Giverny il soggetto \
                scompare sempre di più: resta il colore che vibra sull'acqua.""");
        connect(saturno, urlo, tema, luca, 2600, """
                Due bocche aperte. Il dio di Goya divora, la figura di Munch grida: la pittura smette di \
                rappresentare e mostra direttamente la paura.""");
        connect(bacio, galette, colore, marta, 2250, """
                La festa e l'abbraccio. Renoir sparge la luce a macchie sulla folla, Klimt la condensa in \
                oro attorno a due amanti: due modi di dipingere la felicità.""");

        // un po' di vita sociale sulle nuove connessioni
        comment(c1, marta, "Non avevo mai pensato alla spirale come a un orologio che si scioglie. Ora non riesco più a non vederlo.");
        comment(c3, elena, "Lo specchio di Velázquez nel corridoio dell'Overlook: accostamento bellissimo.");
        comment(c5, luca, "La scena nel ruscello è proprio Millais, anche nei fiori. Grazie per averla messa vicina.");
        comment(c16, giulia, "Il punto di vista di chi viene ritratta: è esattamente quello che manca nel quadro.");
        comment(c24, andrea, "Il ballo del Gattopardo come una corte di Velázquez, con tutto il peso del tempo che passa.");
        List<User> fans = List.of(elena, luca, marta, giulia, andrea, curatore);
        for (Connection c : List.of(c1, c3, c5, c16, c24)) {
            fans.stream().filter(u -> !u.getId().equals(c.getAuthor().getId()))
                    .forEach(u -> likes.save(Like.builder().user(u).connection(c).build()));
        }

        attachImages();
        log.info("Archivio popolato: {} connessioni", connections.count());
    }

    private User user(String username) {
        return users.findByUsername(username).orElseThrow();
    }

    private Category cat(String slug) {
        return categories.findBySlug(slug).orElseGet(() -> categories.save(
                new Category(slug.substring(0, 1).toUpperCase() + slug.substring(1), slug)));
    }

    private Optional<Work> find(WorkType type, String title) {
        return works.findAll().stream()
                .filter(w -> w.getType() == type && w.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    private Work existing(WorkType type, String title) {
        return find(type, title).orElseGet(() -> works.save(new Work(type, title, null, null)));
    }

    private Work film(String title, String director, int year) {
        return find(WorkType.FILM, title).orElseGet(() -> works.save(new Work(WorkType.FILM, title, director, year)));
    }

    private Work art(String title, String artist, int year) {
        return find(WorkType.ARTWORK, title).orElseGet(() -> works.save(new Work(WorkType.ARTWORK, title, artist, year)));
    }

    private Connection connect(Work a, Work b, Category category, User author, long views, String note) {
        Connection c = connections.save(new Connection(a, b, category, note, author));
        jdbc.update("update connections set view_count = ? where id = ?", views, c.getId());
        return c;
    }

    private void comment(Connection c, User author, String text) {
        comments.save(new Comment(c, author, text));
    }

    private boolean fileExists(String imageUrl) {
        if (!imageUrl.startsWith("/images/")) return true;
        return java.nio.file.Files.exists(java.nio.file.Path.of(storageDir, imageUrl.substring("/images/".length())));
    }

    /** Poster dell'anno giusto da TMDB, quadri da Wikipedia; un errore lascia l'opera senza immagine. */
    private void attachImages() {
        for (Work w : works.findAll()) {
            // su Render il disco non è permanente: dopo un nuovo deploy il file può non esserci più
            // anche se il database lo ricorda, e allora va riscaricato
            if (w.getImageUrl() != null && fileExists(w.getImageUrl())) continue;
            try {
                String url = null;
                if (w.getType() == WorkType.FILM && tmdb.isConfigured()) {
                    var found = tmdb.search(w.getTitle());
                    url = found.stream().filter(r -> w.getYear() != null && w.getYear().equals(r.year()))
                            .findFirst().or(() -> found.stream().findFirst())
                            .map(r -> r.imageUrl()).orElse(null);
                } else if (w.getType() == WorkType.ARTWORK) {
                    url = wikipedia.findImageUrl(EN.getOrDefault(w.getTitle(), w.getTitle()), w.getCreator()).orElse(null);
                }
                if (url != null) importService.attachImage(w.getId(), url);
                else log.info("Immagine non trovata per '{}'", w.getTitle());
                // Wikipedia limita le richieste ravvicinate: una breve pausa evita le risposte vuote
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Nessuna immagine per '{}': {}", w.getTitle(), e.getMessage());
            }
        }
        attachStills();
    }

    /**
     * I fotogrammi TMDB sono stati provati e tolti: i film tornano alla locandina e la
     * palette si ricalcola dalla locandina. Pulisce i dati lasciati dalla prova.
     */
    private void attachStills() {
        for (Work w : works.findAll()) {
            if (w.getStillUrl() == null) continue;
            try {
                java.nio.file.Path still = java.nio.file.Path.of(storageDir, w.getStillUrl().substring("/images/".length()));
                java.nio.file.Files.deleteIfExists(still);
                boolean designPalette = w.getImageUrl() != null && w.getImageUrl().startsWith("/images/design-");
                if (!designPalette && w.getImageUrl() != null && w.getImageUrl().startsWith("/images/")) {
                    java.nio.file.Path poster = java.nio.file.Path.of(storageDir, w.getImageUrl().substring("/images/".length()));
                    if (java.nio.file.Files.exists(poster)) {
                        String palette = com.atlante.service.PaletteExtractor.extract(poster, 6);
                        if (palette != null) w.setPalette(palette);
                    }
                }
            } catch (Exception e) {
                log.warn("Pulizia del fotogramma di '{}' non riuscita: {}", w.getTitle(), e.getMessage());
            }
            w.setStillUrl(null);
            works.save(w);
        }
    }
}
