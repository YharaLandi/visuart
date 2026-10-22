package com.atlante.controller;

import com.atlante.service.PhantomService;
import com.atlante.service.PhantomService.OriginRequest;
import com.atlante.service.PhantomService.OriginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Reparto fantasma: lettura pubblica (il frontend lo mostra solo a ottobre, dietro l'avviso +16);
 * le origini le scrivono solo curatori e amministratori.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PhantomController {

    private final PhantomService phantom;

    @GetMapping("/phantom")
    public List<OriginResponse> phantomList() {
        return phantom.list(com.atlante.model.HorrorOrigin.FANTASMA);
    }

    /** Curiosità su film e quadri, tutto l'anno: stesse regole del reparto fantasma. */
    @GetMapping("/curiosities")
    public List<OriginResponse> curiosities() {
        return phantom.list(com.atlante.model.HorrorOrigin.CURIOSITA);
    }

    @PostMapping("/phantom")
    @PreAuthorize("hasAnyRole('CURATOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OriginResponse save(@Valid @RequestBody OriginRequest req) {
        return phantom.save(req);
    }

    @DeleteMapping("/phantom/{id}")
    @PreAuthorize("hasAnyRole('CURATOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        phantom.delete(id);
    }
}
