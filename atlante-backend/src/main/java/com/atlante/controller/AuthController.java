package com.atlante.controller;

import com.atlante.dto.Dtos.AuthResponse;
import com.atlante.dto.Dtos.ForgotPasswordRequest;
import com.atlante.dto.Dtos.LoginRequest;
import com.atlante.dto.Dtos.RegisterRequest;
import com.atlante.dto.Dtos.ResetPasswordRequest;
import com.atlante.service.AuthService;
import com.atlante.service.PasswordResetService;
import com.atlante.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Revoca il token della richiesta: da qui in poi non vale più, anche se non è scaduto. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
        }
    }

    /** Revoca il token in uso e ne emette uno nuovo. */
    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public AuthResponse refresh(@RequestHeader("Authorization") String authorization,
                                @AuthenticationPrincipal UserDetails principal) {
        return authService.refresh(userService.findByUsername(principal.getUsername()),
                authorization.substring(7));
    }

    /** Risposta sempre uguale, che l'email sia registrata o no. */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.requestReset(req.email());
        return Map.of("message",
                "Se l'indirizzo è registrato, riceverai a breve una mail con il link per reimpostare la password.");
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.token(), req.password());
    }
}
