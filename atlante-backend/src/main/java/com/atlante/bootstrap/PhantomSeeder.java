package com.atlante.bootstrap;

import com.atlante.model.HorrorOrigin;
import com.atlante.model.Work;
import com.atlante.model.WorkType;
import com.atlante.repository.HorrorOriginRepository;
import com.atlante.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Reparto fantasma (film e quadri horror) e curiosità generali, curate dalla redazione. Le curiosità sono fatti documentati e senza
 * dettagli macabri; per il true crime c'è solo il nome del caso e il link a Wikipedia.
 * Gira prima dell'archivio, così le locandine dei film nuovi arrivano con le altre immagini.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE - 7)
@RequiredArgsConstructor
public class PhantomSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PhantomSeeder.class);

    private final WorkRepository works;
    private final HorrorOriginRepository origins;

    @Override
    public void run(String... args) {
        long before = origins.count();

        origin("Nosferatu il vampiro", "F. W. Murnau", 1922, "LETTERATURA", "Dracula di Bram Stoker", 1897,
                "Era un adattamento non autorizzato del romanzo: gli eredi di Stoker fecero causa e un tribunale "
                        + "ordinò di distruggere le copie. Alcune si salvarono, ed è per questo che oggi lo vediamo.",
                "https://it.wikipedia.org/wiki/Nosferatu_il_vampiro");
        origin("Dracula di Bram Stoker", "Francis Ford Coppola", 1992, "STORIA", "Vlad III di Valacchia", 1456,
                "Il conte prende il nome dal principe di Valacchia del Quattrocento, detto l'Impalatore; "
                        + "Stoker ne aveva letto in un libro sulla storia dei Carpazi.",
                "https://it.wikipedia.org/wiki/Vlad_III_di_Valacchia");
        origin("Psyco", "Alfred Hitchcock", 1960, "TRUE_CRIME", "Il caso Ed Gein", 1957, null,
                "https://it.wikipedia.org/wiki/Ed_Gein");
        origin("Non aprite quella porta", "Tobe Hooper", 1974, "TRUE_CRIME", "Il caso Ed Gein", 1957, null,
                "https://it.wikipedia.org/wiki/Ed_Gein");
        origin("The Wicker Man", "Robin Hardy", 1973, "FOLKLORE", "Il fantoccio di vimini dei druidi", -52,
                "L'immagine del gigante di vimini viene dal De bello Gallico di Giulio Cesare, che la attribuisce "
                        + "ai riti dei druidi della Gallia. Il film la porta su un'isola scozzese.",
                "https://en.wikipedia.org/wiki/Wicker_man");
        origin("L'esorcista", "William Friedkin", 1973, "CRONACA", "L'esorcismo del 1949", 1949,
                "William Peter Blatty, autore del romanzo, partì da un caso raccontato dai giornali americani "
                        + "nel 1949: l'esorcismo di un ragazzo del Maryland.",
                "https://en.wikipedia.org/wiki/Exorcism_of_Roland_Doe");
        origin("Lo squalo", "Steven Spielberg", 1975, "CRONACA", "Gli attacchi di squalo del 1916", 1916,
                "Tra le ispirazioni del romanzo di Peter Benchley si citano spesso gli attacchi di squalo "
                        + "dell'estate 1916 sulla costa del New Jersey.",
                "https://en.wikipedia.org/wiki/Jersey_Shore_shark_attacks_of_1916");
        origin("Suspiria", "Dario Argento", 1977, "LETTERATURA", "Suspiria de profundis", 1845,
                "Le tre Madri della trilogia di Argento vengono da una prosa di Thomas De Quincey, "
                        + "le Nostre Signore del Dolore.",
                "https://en.wikipedia.org/wiki/Suspiria_de_Profundis");
        origin("Amityville Horror", "Stuart Rosenberg", 1979, "TRUE_CRIME", "La casa di Amityville", 1974, null,
                "https://en.wikipedia.org/wiki/The_Amityville_Horror");
        origin("Shining", "Stanley Kubrick", 1980, "LETTERATURA", "Lo Stanley Hotel", 1974,
                "Stephen King immaginò la storia dopo una notte allo Stanley Hotel, in Colorado, quasi vuoto "
                        + "a fine stagione: alloggiava nella stanza 217.",
                "https://it.wikipedia.org/wiki/Stanley_Hotel");
        origin("Candyman", "Bernard Rose", 1992, "LEGGENDA", "La leggenda di Bloody Mary", null,
                "Una leggenda metropolitana: un nome ripetuto davanti allo specchio evoca chi non dovrebbe tornare. "
                        + "Il film nasce da un racconto di Clive Barker.",
                "https://en.wikipedia.org/wiki/Bloody_Mary_(folklore)");
        origin("The Conjuring - L'evocazione", "James Wan", 2013, "LEGGENDA", "I racconti dei coniugi Warren", 1971,
                "Parte dai racconti di Ed e Lorraine Warren, investigatori del paranormale, su una casa di campagna "
                        + "nel Rhode Island all'inizio degli anni Settanta.",
                "https://en.wikipedia.org/wiki/Ed_and_Lorraine_Warren");
        origin("The Witch", "Robert Eggers", 2015, "STORIA", "La caccia alle streghe nel New England", 1692,
                "Molti dialoghi riprendono diari, lettere e atti dei processi del Seicento: Eggers voleva che "
                        + "i personaggi parlassero come i coloni puritani.",
                "https://en.wikipedia.org/wiki/Witch_trials_in_the_early_modern_period");
        origin("Midsommar - Il villaggio dei dannati", "Ari Aster", 2019, "FOLKLORE", "La festa di mezza estate svedese", null,
                "Prende i fiori, le danze attorno al palo e la luce che non tramonta del Midsommar svedese "
                        + "e li trasforma in una comunità immaginaria.",
                "https://it.wikipedia.org/wiki/Midsommar");
        origin("The Lighthouse", "Robert Eggers", 2019, "LEGGENDA", "La leggenda del faro di Smalls", 1801,
                "Tra le ispirazioni c'è una storia di guardiani del faro gallese di Smalls, rimasti isolati "
                        + "per settimane durante una tempesta, e le superstizioni dei marinai.",
                "https://en.wikipedia.org/wiki/Smalls_Lighthouse");

        // quadri del reparto fantasma
        art("Saturno che divora i suoi figli", "Francisco Goya", 1823, "MITO", "Il mito di Crono", null,
                "Goya lo dipinse direttamente sui muri della sua casa, la Quinta del Sordo, tra le Pitture nere: "
                        + "non era destinato al pubblico. Solo più tardi fu staccato e portato su tela.",
                "https://it.wikipedia.org/wiki/Saturno_che_divora_i_suoi_figli", HorrorOrigin.FANTASMA);
        art("L’urlo", "Edvard Munch", 1893, "STORIA", "Una passeggiata al tramonto a Oslo", 1892,
                "Munch annotò nel diario una sera in cui il cielo diventò rosso mentre camminava con due amici "
                        + "sul fiordo: da quel ricordo nacquero diverse versioni del quadro.",
                "https://it.wikipedia.org/wiki/L%27urlo", HorrorOrigin.FANTASMA);
        art("Il giardino delle delizie", "Hieronymus Bosch", 1500, "STORIA", "Le visioni del Medioevo fiammingo", null,
                "Il pannello di destra mostra un inferno pieno di strumenti musicali giganti: uno spartito dipinto "
                        + "su una figura è stato perfino trascritto e suonato.",
                "https://it.wikipedia.org/wiki/Trittico_del_Giardino_delle_delizie", HorrorOrigin.FANTASMA);
        art("L’incubo", "Johann Heinrich Füssli", 1781, "FOLKLORE", "Il demone della notte del folklore", null,
                "Il folletto seduto sulla dormiente è il demone che nelle credenze popolari opprimeva chi dormiva. "
                        + "Il quadro fece scandalo e venne copiato e parodiato per decenni.",
                "https://it.wikipedia.org/wiki/L%27incubo", HorrorOrigin.FANTASMA);

        // curiosità generali, tutto l'anno
        curiosity("Nighthawks", WorkType.ARTWORK, "STORIA", "Un diner del Greenwich Village", 1942,
                "Hopper disse che il quadro gli era stato suggerito da un ristorante su Greenwich Avenue, a New York, "
                        + "dove due strade si incontrano.",
                "https://it.wikipedia.org/wiki/Nighthawks");
        curiosity("Viandante sul mare di nebbia", WorkType.ARTWORK, "STORIA", "Le montagne di arenaria dell'Elba", 1818,
                "Il paesaggio non esiste così: Friedrich montò in studio vedute diverse delle montagne dell'Elba, "
                        + "in Sassonia, come un collage.",
                "https://it.wikipedia.org/wiki/Viandante_sul_mare_di_nebbia");
        curiosity("La grande onda di Kanagawa", WorkType.ARTWORK, "STORIA", "Le Trentasei vedute del Monte Fuji", 1831,
                "È una stampa di una serie dedicata al Fuji: il monte è il piccolo cono bianco sullo sfondo, "
                        + "quasi nascosto dall'onda.",
                "https://it.wikipedia.org/wiki/La_grande_onda_di_Kanagawa");
        curiosity("Notte stellata", WorkType.ARTWORK, "STORIA", "La finestra di Saint-Rémy", 1889,
                "Van Gogh la dipinse guardando dalla finestra della sua stanza nella casa di cura di Saint-Rémy; "
                        + "il paese in basso è in buona parte inventato.",
                "https://it.wikipedia.org/wiki/Notte_stellata");
        curiosity("Las Meninas", WorkType.ARTWORK, "STORIA", "Lo studio di Velázquez", 1656,
                "Velázquez si è dipinto al lavoro, a sinistra, davanti a una grande tela; nello specchio in fondo "
                        + "compaiono i re di Spagna.",
                "https://it.wikipedia.org/wiki/Las_Meninas");
        curiosity("2001: Odissea nello spazio", WorkType.FILM, "LETTERATURA", "La sentinella di Arthur C. Clarke", 1951,
                "Il film nasce da un breve racconto di Clarke su un oggetto lasciato sulla Luna; il romanzo venne "
                        + "scritto in parallelo al film.",
                "https://it.wikipedia.org/wiki/La_sentinella_(racconto)");
        curiosity("Blade Runner 2049", WorkType.FILM, "LETTERATURA", "Il romanzo di Philip K. Dick", 1968,
                "Il mondo di Blade Runner viene da Ma gli androidi sognano pecore elettriche?, romanzo di Philip K. Dick.",
                "https://it.wikipedia.org/wiki/Ma_gli_androidi_sognano_pecore_elettriche%3F");
        curiosity("Grand Budapest Hotel", WorkType.FILM, "LETTERATURA", "Gli scritti di Stefan Zweig", null,
                "Wes Anderson ha dichiarato di essersi ispirato ai racconti e ai ricordi di Stefan Zweig sull'Europa "
                        + "prima delle guerre.",
                "https://it.wikipedia.org/wiki/Grand_Budapest_Hotel");
        curiosity("Lost in Translation", WorkType.FILM, "STORIA", "Il Park Hyatt di Tokyo", 2003,
                "Gran parte del film è girata nell'hotel Park Hyatt di Tokyo, dove Sofia Coppola aveva soggiornato "
                        + "in passato.",
                "https://it.wikipedia.org/wiki/Lost_in_Translation_-_L%27amore_tradotto");
        curiosity("Il favoloso mondo di Amélie", WorkType.FILM, "STORIA", "Il caffè di Montmartre", 2001,
                "Il bar dove lavora Amélie esiste davvero: è il Café des Deux Moulins, a Montmartre, a Parigi.",
                "https://it.wikipedia.org/wiki/Il_favoloso_mondo_di_Am%C3%A9lie");

        log.info("Reparto fantasma e curiosità: {} voci ({} nuove)", origins.count(), origins.count() - before);
    }

    private void origin(String title, String director, int year, String type, String originTitle,
                        Integer originYear, String trivia, String url) {
        add(work(WorkType.FILM, title, director, year), type, originTitle, originYear, trivia, url, HorrorOrigin.FANTASMA);
    }

    private void art(String title, String artist, int year, String type, String originTitle,
                     Integer originYear, String trivia, String url, String section) {
        add(work(WorkType.ARTWORK, title, artist, year), type, originTitle, originYear, trivia, url, section);
    }

    /** Curiosità su un'opera che è già nell'archivio (se non c'è, la voce si salta). */
    private void curiosity(String title, WorkType kind, String type, String originTitle,
                           Integer originYear, String trivia, String url) {
        find(kind, title).ifPresent(w -> add(w, type, originTitle, originYear, trivia, url, HorrorOrigin.CURIOSITA));
    }

    private java.util.Optional<Work> find(WorkType kind, String title) {
        return works.findAll().stream()
                .filter(w -> w.getType() == kind && w.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    private Work work(WorkType kind, String title, String creator, int year) {
        return find(kind, title).orElseGet(() -> works.save(new Work(kind, title, creator, year)));
    }

    /** Una voce per opera: se esiste già, non si tocca. */
    private void add(Work work, String type, String originTitle, Integer originYear, String trivia, String url, String section) {
        if (origins.findByFilmId(work.getId()).isPresent()) return;
        HorrorOrigin o = new HorrorOrigin(work, type, originTitle, originYear, trivia, url);
        o.setSection(section);
        origins.save(o);
    }
}
