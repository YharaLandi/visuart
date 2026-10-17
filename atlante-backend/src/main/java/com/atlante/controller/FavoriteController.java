package com.atlante.controller;

import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<WorkResponse> list(Authentication auth) {
        return favoriteService.list(auth.getName());
    }

    @PostMapping("/{workId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@PathVariable Long workId, Authentication auth) {
        favoriteService.add(auth.getName(), workId);
    }

    @DeleteMapping("/{workId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long workId, Authentication auth) {
        favoriteService.remove(auth.getName(), workId);
    }
}
