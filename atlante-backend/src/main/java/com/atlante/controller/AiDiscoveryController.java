package com.atlante.controller;

import com.atlante.dto.Dtos.AiDiscoveryResponse;
import com.atlante.model.WorkType;
import com.atlante.service.AiDiscoveryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiDiscoveryController {

    private final AiDiscoveryService aiDiscoveryService;

    public AiDiscoveryController(AiDiscoveryService aiDiscoveryService) {
        this.aiDiscoveryService = aiDiscoveryService;
    }

    @GetMapping("/discover")
    public AiDiscoveryResponse discover(@RequestParam String q,
                                        @RequestParam(defaultValue = "FILM") WorkType type) {
        if (q == null || q.isBlank()) {
            throw new IllegalArgumentException("Inserisci un titolo da cercare");
        }
        return aiDiscoveryService.discover(q, type);
    }
}
