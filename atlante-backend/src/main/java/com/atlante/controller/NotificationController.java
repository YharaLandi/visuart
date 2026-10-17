package com.atlante.controller;

import com.atlante.dto.Dtos.NotificationResponse;
import com.atlante.service.NotificationService;
import com.atlante.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Le notifiche nuove arrivano in tempo reale su /user/queue/notifications; qui lo storico. */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return notificationService.latest(userService.findByUsername(principal.getUsername()));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal UserDetails principal) {
        return Map.of("unread", notificationService.unread(userService.findByUsername(principal.getUsername())));
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal UserDetails principal) {
        notificationService.markAllRead(userService.findByUsername(principal.getUsername()));
    }
}
