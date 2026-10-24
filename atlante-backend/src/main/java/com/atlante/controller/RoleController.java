package com.atlante.controller;

import com.atlante.dto.Dtos.RoleRequest;
import com.atlante.service.RoleService;
import com.atlante.service.RoleService.RoleView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestione dei ruoli (come RuoloController di showroom): creare, modificare, eliminare. */
@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roles;

    @GetMapping
    public List<RoleView> list() {
        return roles.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleView create(@Valid @RequestBody RoleRequest req) {
        return roles.create(req.name(), req.description());
    }

    @PutMapping("/{id}")
    public RoleView update(@PathVariable Long id, @Valid @RequestBody RoleRequest req) {
        return roles.update(id, req.name(), req.description());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roles.delete(id);
    }
}
