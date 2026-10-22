package com.atlante.service;

import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.HorrorOrigin;
import com.atlante.model.Work;
import com.atlante.repository.HorrorOriginRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reparto fantasma: i film horror e la loro origine. Le regole sul true crime stanno qui, non
 * nell'interfaccia: nessun racconto, solo un link a Wikipedia, e un filtro di parole sensibili
 * su tutti i testi come rete di sicurezza.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PhantomService {

    private final HorrorOriginRepository origins;
    private final WorkService workService;

    /** Solo Wikipedia italiana o inglese: una fonte verificabile, non un sito qualsiasi. */
    private static final Pattern WIKIPEDIA = Pattern.compile("^https://(it|en)\\.wikipedia\\.org/wiki/[^\\s?#]+$");

    /** Radici di parole che nel reparto non si scrivono: niente dettagli macabri su fatti reali. */
    private static final List<String> SENSITIVE = List.of(
            "omicid", "uccis", "ammazz", "assassin", "strage", "massacr", "cadaver", "smembr", "squart",
            "decapit", "mutil", "tortur", "stupr", "violenz", "sangu", "vittim", "suicid", "pedofil",
            "murder", "killed", "corpse", "dismember", "torture", "rape", "victim", "suicide");

    public record OriginRequest(@NotNull Long filmId,
                                @NotBlank String type,
                                @NotBlank @Size(max = 160) String originTitle,
                                Integer originYear,
                                @Size(max = 500) String trivia,
                                @NotBlank String wikipediaUrl,
                                String section) {
    }

    public record OriginResponse(Long id, WorkResponse film, String type, String originTitle,
                                 Integer originYear, String trivia, String wikipediaUrl, String section) {
        static OriginResponse from(HorrorOrigin o) {
            return new OriginResponse(o.getId(), WorkResponse.from(o.getFilm()), o.getType(), o.getOriginTitle(),
                    o.getOriginYear(), o.getTrivia(), o.getWikipediaUrl(), o.getSection());
        }
    }

    @Transactional(readOnly = true)
    public List<OriginResponse> list(String section) {
        return origins.findBySection(section).stream().map(OriginResponse::from).toList();
    }

    public OriginResponse save(OriginRequest req) {
        // "film" per ragioni storiche: può essere un film o un quadro
        Work film = workService.getEntity(req.filmId());
        String section = req.section() == null || req.section().isBlank()
                ? HorrorOrigin.FANTASMA : req.section().trim().toUpperCase(Locale.ROOT);
        if (!HorrorOrigin.FANTASMA.equals(section) && !HorrorOrigin.CURIOSITA.equals(section)) {
            throw new IllegalArgumentException("Sezione non valida: FANTASMA o CURIOSITA");
        }
        String type = req.type().trim().toUpperCase(Locale.ROOT);
        if (!HorrorOrigin.TYPES.contains(type)) {
            throw new IllegalArgumentException("Tipo di origine non valido: usa " + String.join(", ", HorrorOrigin.TYPES));
        }
        String url = req.wikipediaUrl().trim();
        if (!WIKIPEDIA.matcher(url).matches()) {
            throw new IllegalArgumentException("Serve un link a una voce di Wikipedia (it.wikipedia.org o en.wikipedia.org).");
        }
        String trivia = req.trivia() == null || req.trivia().isBlank() ? null : req.trivia().trim();
        if (HorrorOrigin.TRUE_CRIME.equals(type) && trivia != null) {
            throw new IllegalArgumentException("Per i casi di cronaca nera non si racconta la storia: lascia solo il link a Wikipedia.");
        }
        requireNoSensitiveWords(req.originTitle());
        requireNoSensitiveWords(trivia);

        HorrorOrigin o = origins.findByFilmId(film.getId())
                .orElseGet(() -> new HorrorOrigin(film, type, req.originTitle().trim(), req.originYear(), trivia, url));
        o.setType(type);
        o.setSection(section);
        o.setOriginTitle(req.originTitle().trim());
        o.setOriginYear(req.originYear());
        o.setTrivia(trivia);
        o.setWikipediaUrl(url);
        return OriginResponse.from(origins.save(o));
    }

    public void delete(Long id) {
        if (!origins.existsById(id)) throw new ResourceNotFoundException("Origine non trovata: " + id);
        origins.deleteById(id);
    }

    /** Confronto senza accenti e maiuscole, così "Vìttima" o "VITTIMA" non passano. */
    static void requireNoSensitiveWords(String text) {
        if (text == null) return;
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (String root : SENSITIVE) {
            if (plain.contains(root)) {
                throw new IllegalArgumentException("Nel reparto fantasma non si usano parole come «" + root
                        + "…»: racconta l'origine senza dettagli macabri.");
            }
        }
    }
}
