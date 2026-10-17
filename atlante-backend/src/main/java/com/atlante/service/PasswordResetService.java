package com.atlante.service;

import com.atlante.config.AsyncConfig;
import com.atlante.model.PasswordResetToken;
import com.atlante.model.User;
import com.atlante.repository.PasswordResetTokenRepository;
import com.atlante.repository.UserRepository;
import com.atlante.security.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class PasswordResetService {

    /** Evento interno: la mail parte solo dopo che il token è davvero salvato. */
    public record ResetRequested(String email, String displayName, String token) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final TokenService tokenService;
    private final ApplicationEventPublisher events;
    private final String frontendUrl;
    private final long expirationMinutes;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                TokenService tokenService,
                                ApplicationEventPublisher events,
                                @Value("${app.frontend-url}") String frontendUrl,
                                @Value("${app.password-reset.expiration-minutes}") long expirationMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.tokenService = tokenService;
        this.events = events;
        this.frontendUrl = frontendUrl;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Se l'email esiste prepara un nuovo token (annullando i precedenti) e manda la mail.
     * Se non esiste non fa nulla: la risposta HTTP è identica, così non si scopre chi è iscritto.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).ifPresent(user -> {
            tokenRepository.deleteByUser(user);
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            tokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(TokenHasher.sha256(token))
                    .expiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)))
                    .build());
            events.publishEvent(new ResetRequested(user.getEmail(), user.getDisplayName(), token));
        });
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResetRequested(ResetRequested event) {
        String link = frontendUrl + "/reimposta-password?token=" + event.token();
        mailService.sendPasswordReset(event.email(), event.displayName(), link, expirationMinutes);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken reset = tokenRepository.findByTokenHash(TokenHasher.sha256(token))
                .filter(t -> t.isUsable(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Link non valido o scaduto: richiedi un nuovo reset della password"));
        User user = reset.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        reset.setUsedAt(Instant.now());
        // chi aveva rubato la sessione viene buttato fuori
        tokenService.revokeAll(user);
    }
}
