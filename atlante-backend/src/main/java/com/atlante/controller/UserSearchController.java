package com.atlante.controller;

import com.atlante.dto.Dtos.UserSummary;
import com.atlante.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Trova le persone a cui scrivere: restituisce solo username e nome, mai l'email. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class UserSearchController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    public List<UserSummary> search(@RequestParam(defaultValue = "") String q,
                                    @AuthenticationPrincipal UserDetails principal) {
        String query = q.trim();
        if (query.length() < 2) {
            return List.of();
        }
        return userRepository.searchOthers(query, principal.getUsername(), Limit.of(10)).stream()
                .map(u -> new UserSummary(u.getUsername(), u.getDisplayName()))
                .toList();
    }
}
