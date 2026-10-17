package com.atlante.controller;

import com.atlante.dto.Dtos.GraphResponse;
import com.atlante.service.GraphService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping
    public GraphResponse graph(@RequestParam(required = false) Long workId,
                               @RequestParam(defaultValue = "2") int depth,
                               @RequestParam(required = false) String category) {
        return graphService.buildGraph(workId, depth, category);
    }
}
