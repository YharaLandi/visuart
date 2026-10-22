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
 * Reparto fantasma: film horror e la loro origine. Le curiosità sono fatti documentati e senza
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
        if (origins.count() > 0) return;

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

        log.info("Reparto fantasma: {} film con la loro origine", origins.count());
    }

    private void origin(String title, String director, int year, String type, String originTitle,
                        Integer originYear, String trivia, String url) {
        Work film = works.findAll().stream()
                .filter(w -> w.getType() == WorkType.FILM && w.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElseGet(() -> works.save(new Work(WorkType.FILM, title, director, year)));
        origins.save(new HorrorOrigin(film, type, originTitle, originYear, trivia, url));
    }
}
