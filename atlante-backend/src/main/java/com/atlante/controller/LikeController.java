package com.atlante.controller;

import com.atlante.dto.Dtos.LikeResponse;
import com.atlante.model.Connection;
import com.atlante.model.User;
import com.atlante.service.ConnectionService;
import com.atlante.service.LikeService;
import com.atlante.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections/{connectionId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final ConnectionService connectionService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<LikeResponse> status(@PathVariable Long connectionId,
                                               @AuthenticationPrincipal UserDetails principal) {
        connectionService.getEntity(connectionId);
        boolean liked = principal != null
                && likeService.likedBy(principal.getUsername(), List.of(connectionId)).contains(connectionId);
        return ResponseEntity.ok(new LikeResponse(likeService.count(connectionId), liked));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LikeResponse> addLike(@PathVariable Long connectionId,
                                                @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByUsername(principal.getUsername());
        Connection connection = connectionService.getEntity(connectionId);
        likeService.addLike(user, connection);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new LikeResponse(likeService.count(connectionId), true));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeLike(@PathVariable Long connectionId,
                                           @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByUsername(principal.getUsername());
        Connection connection = connectionService.getEntity(connectionId);
        likeService.removeLike(user, connection);
        return ResponseEntity.noContent().build();
    }
}
