package com.atlante.service;

import com.atlante.dto.Dtos.AuthResponse;
import com.atlante.dto.Dtos.LoginRequest;
import com.atlante.dto.Dtos.RegisterRequest;
import com.atlante.model.Role;
import com.atlante.model.User;
import com.atlante.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email già registrata");
        }
        String username = request.username() != null && !request.username().isBlank()
                ? request.username().trim()
                : generateUsername(email);
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username già in uso");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(Role.USER)
                .build();

        return toAuthResponse(userRepository.save(user));
    }

    /** Il login accetta indifferentemente email o username nel campo "email". */
    public AuthResponse login(LoginRequest request) {
        String identifier = request.email().trim();
        User user = userRepository.findByEmail(normalizeEmail(identifier))
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new BadCredentialsException("Credenziali non valide"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.password()));
        return toAuthResponse(user);
    }

    /** Le email si confrontano senza distinguere maiuscole e minuscole. */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + (++suffix);
        }
        return candidate;
    }

    /** Revoca il token in uso e ne emette uno nuovo con scadenza rinnovata. */
    public AuthResponse refresh(User user, String currentToken) {
        tokenService.revoke(currentToken);
        return toAuthResponse(user);
    }

    public void logout(String currentToken) {
        tokenService.revoke(currentToken);
    }

    public AuthResponse toAuthResponse(User user) {
        return new AuthResponse(tokenService.issue(user), user.getUsername(),
                user.getEmail(), user.getDisplayName(), user.getRole().name());
    }
}
