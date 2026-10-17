package com.atlante.service;

import com.atlante.model.IssuedToken;
import com.atlante.model.User;
import com.atlante.repository.IssuedTokenRepository;
import com.atlante.security.JwtUtil;
import com.atlante.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    private final JwtUtil jwtUtil;
    private final IssuedTokenRepository repository;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /** Emette un JWT e lo registra, così può essere revocato prima della scadenza. */
    public String issue(User user) {
        String token = jwtUtil.generateToken(user.getUsername());
        repository.save(IssuedToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(token))
                .expiresAt(Instant.now().plusMillis(expirationMs))
                .build());
        return token;
    }

    /** Firma valida, emesso da noi, non revocato e non scaduto. */
    @Transactional(readOnly = true)
    public boolean isActive(String token) {
        return jwtUtil.isTokenValid(token)
                && repository.isActive(TokenHasher.sha256(token), Instant.now());
    }

    public void revoke(String token) {
        repository.findByTokenHash(TokenHasher.sha256(token)).ifPresent(t -> t.setRevoked(true));
    }

    /** Logout da tutti i dispositivi: usato dopo un cambio o un reset della password. */
    public void revokeAll(User user) {
        repository.revokeAllOf(user);
    }

    /** Pulizia notturna dei token scaduti. */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpired() {
        repository.deleteExpired(Instant.now());
    }
}
