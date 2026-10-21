package com.atlante.controller;

import com.atlante.dto.Dtos.ConnectionRequest;
import com.atlante.dto.Dtos.ConnectionResponse;
import com.atlante.service.ConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @GetMapping
    public List<ConnectionResponse> findAll(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) String sort,
                                            @AuthenticationPrincipal UserDetails principal) {
        String viewer = principal != null ? principal.getUsername() : null;
        if ("popular".equalsIgnoreCase(sort)) return connectionService.findAllByPopularity(category, viewer);
        return "views".equalsIgnoreCase(sort)
                ? connectionService.findAllByViews(category, viewer)
                : connectionService.findAll(category, viewer);
    }

    /** Ricerca paginata: ?q=&category=&type=FILM&author=&sort=popular|views|recent|oldest&page=0&size=12 */
    @GetMapping("/search")
    public com.atlante.dto.Dtos.PageResponse<ConnectionResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) com.atlante.model.WorkType type,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserDetails principal) {
        return connectionService.search(q, category, type, author, sort, page, size,
                principal != null ? principal.getUsername() : null);
    }

    @GetMapping("/{id}")
    public ConnectionResponse findById(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        return connectionService.findById(id, principal != null ? principal.getUsername() : null);
    }

    @PostMapping("/{id}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerView(@PathVariable Long id) {
        connectionService.registerView(id);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionResponse create(@Valid @RequestBody ConnectionRequest req,
                                     @AuthenticationPrincipal UserDetails principal) {
        return connectionService.create(req, principal.getUsername());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ConnectionResponse update(@PathVariable Long id,
                                     @Valid @RequestBody ConnectionRequest req,
                                     @AuthenticationPrincipal UserDetails principal) {
        return connectionService.update(id, req, principal.getUsername());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        connectionService.delete(id, principal.getUsername());
    }
}
