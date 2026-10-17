package com.atlante.controller;

import com.atlante.dto.Dtos.AiAnalysisResponse;
import com.atlante.service.AiAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/connections/{connectionId}/ai-analysis")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping
    public AiAnalysisResponse analyze(@PathVariable Long connectionId) {
        return aiAnalysisService.analyze(connectionId);
    }
}
