package com.atlante.controller;

import com.atlante.dto.Dtos.ChangeRoleRequest;
import com.atlante.dto.Dtos.PageResponse;
import com.atlante.dto.Dtos.UserResponse;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.User;
import com.atlante.repository.UserRepository;
import com.atlante.service.AccountService;
import com.atlante.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Gestione utenti riservata agli ADMIN (come UtenteController di SocialNetwork). */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final TokenService tokenService;
    private final com.atlante.service.RoleService roleService;

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "") String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("username"));
        return PageResponse.of(userRepository
                .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable)
                .map(UserResponse::from));
    }

    @PatchMapping("/{id}/role")
    @Transactional
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest req,
                                   @AuthenticationPrincipal UserDetails principal) {
        User user = find(id);
        if (user.getUsername().equals(principal.getUsername())) {
            throw new IllegalArgumentException("Non puoi cambiare il tuo stesso ruolo");
        }
        // il ruolo principale sostituisce i precedenti; USER resta sempre
        return UserResponse.from(roleService.assign(user, java.util.List.of(req.role())));
    }

    /** Assegna all'utente esattamente questi ruoli, compresi quelli creati dall'admin. */
    @PutMapping("/{id}/roles")
    @Transactional
    public UserResponse assignRoles(@PathVariable Long id, @Valid @RequestBody com.atlante.dto.Dtos.AssignRolesRequest req,
                                    @AuthenticationPrincipal UserDetails principal) {
        User user = find(id);
        if (user.getUsername().equals(principal.getUsername()) && !req.roles().contains(com.atlante.model.Role.ADMIN)) {
            throw new IllegalArgumentException("Non puoi toglierti il ruolo di amministratore");
        }
        return UserResponse.from(roleService.assign(user, req.roles()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = find(id);
        if (user.getUsername().equals(principal.getUsername())) {
            throw new IllegalArgumentException("Per eliminare il tuo account usa il profilo");
        }
        accountService.delete(user);
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato: " + id));
    }
}
