package com.atlante.controller;

import com.atlante.dto.Dtos.AffinityResponse;
import com.atlante.dto.Dtos.WorkRequest;
import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.model.WorkType;
import com.atlante.service.AffinityService;
import com.atlante.service.WorkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/works")
public class WorkController {

    private final WorkService workService;
    private final AffinityService affinityService;

    public WorkController(WorkService workService, AffinityService affinityService) {
        this.workService = workService;
        this.affinityService = affinityService;
    }

    @GetMapping("/{id}/affinities")
    public List<AffinityResponse> affinities(@PathVariable Long id,
                                             @RequestParam(defaultValue = "3") int limit) {
        return affinityService.affinities(id, limit);
    }

    @GetMapping
    public List<WorkResponse> findAll(@RequestParam(required = false) WorkType type,
                                      @RequestParam(required = false) String q) {
        return workService.findAll(type, q);
    }

    @GetMapping("/{id}")
    public WorkResponse findById(@PathVariable Long id) {
        return workService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkResponse create(@Valid @RequestBody WorkRequest req) {
        return workService.create(req);
    }

    /** Correggere o eliminare opere tocca le connessioni di tutti: solo gli ADMIN. */
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public WorkResponse update(@PathVariable Long id, @Valid @RequestBody WorkRequest req) {
        return workService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workService.delete(id);
    }
}
