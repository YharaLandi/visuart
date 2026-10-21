package com.atlante.service;


import com.atlante.dto.Dtos.AiAnalysisResponse;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.Connection;
import com.atlante.model.Work;
import com.atlante.model.WorkType;
import com.atlante.repository.ConnectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiAnalysisService {

    private final ConnectionRepository connectionRepository;
    private final LlmService llm;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Long, AiAnalysisResponse> cache = new ConcurrentHashMap<>();

    public AiAnalysisService(ConnectionRepository connectionRepository, LlmService llm) {
        this.connectionRepository = connectionRepository;
        this.llm = llm;
    }

    @Transactional(readOnly = true)
    public AiAnalysisResponse analyze(Long connectionId) {
        AiAnalysisResponse cached = cache.get(connectionId);
        if (cached != null) {
            return cached;
        }

        Connection conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connessione non trovata: " + connectionId));
        // la coppia può essere film–opera, film–film o opera–opera: nelle coppie miste il film viene
        // prima, come nell'interfaccia. filmThemes/artworkThemes restano i nomi dei campi per la
        // prima e la seconda opera, così il contratto dell'API non cambia.
        boolean mixed = conn.getSource().getType() != conn.getTarget().getType();
        boolean swap = mixed && conn.getSource().getType() == WorkType.ARTWORK;
        Work first = swap ? conn.getTarget() : conn.getSource();
        Work second = swap ? conn.getSource() : conn.getTarget();
        // se chi scrive ha scelto le sue palette, la lettura parte da quelle
        String firstPalette = paletteOr(swap ? conn.getTargetPalette() : conn.getSourcePalette(), first);
        String secondPalette = paletteOr(swap ? conn.getSourcePalette() : conn.getTargetPalette(), second);

        String prompt = """
                Sei un assistente per la critica cinematografica e artistica.
                Analizza l'accostamento cromatico tra due opere: %s.

                PRIMA OPERA (%s): "%s" di %s (%s), palette dominante: %s
                SECONDA OPERA (%s): "%s" di %s (%s), palette dominante: %s
                Categoria della connessione critica: %s

                Rispondi SOLO con JSON valido, senza markdown, in questo formato:
                {"filmThemes":["tema1","tema2","tema3"],"artworkThemes":["tema1","tema2","tema3"],"comparison":"..."}

                filmThemes (prima opera) e artworkThemes (seconda opera): tre aggettivi o temi evocati dalla rispettiva palette
                (es. "notturno", "febbrile", "domestico"). comparison: un paragrafo in italiano,
                massimo 100 parole, che confronta le due palette sul piano cromatico e tematico,
                con tono da critica colta ma accessibile. Non ripetere i dati forniti.
                """.formatted(
                pairLabel(first, second),
                kind(first), first.getTitle(), first.getCreator(), first.getYear(), firstPalette,
                kind(second), second.getTitle(), second.getCreator(), second.getYear(), secondPalette,
                conn.getCategory().getName());

        String raw = llm.generate(prompt).trim();
        AiAnalysisResponse result = parse(raw);
        cache.put(connectionId, result);
        return result;
    }

    private static String paletteOr(String chosen, Work w) {
        return chosen != null && !chosen.isBlank() ? chosen : w.getPalette();
    }

    private static String kind(Work w) {
        return w.getType() == WorkType.FILM ? "film" : "opera d'arte";
    }

    private static String pairLabel(Work a, Work b) {
        if (a.getType() != b.getType()) return "un film e un'opera d'arte";
        return a.getType() == WorkType.FILM ? "due film" : "due opere d'arte";
    }

    private AiAnalysisResponse parse(String raw) {
        String cleaned = raw.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        try {
            JsonNode n = mapper.readTree(cleaned);
            return new AiAnalysisResponse(
                    toList(n.get("filmThemes")),
                    toList(n.get("artworkThemes")),
                    n.path("comparison").asText(""));
        } catch (Exception e) {
            // se il modello non ha rispettato il formato, mostra comunque il testo
            return new AiAnalysisResponse(List.of(), List.of(), cleaned);
        }
    }

    private List<String> toList(JsonNode array) {
        List<String> out = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(x -> out.add(x.asText()));
        }
        return out;
    }
}
