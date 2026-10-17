package com.atlante.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Ogni JWT emesso viene registrato (solo come hash SHA-256): un token è valido
 * se è stato emesso da noi e non è stato revocato con logout, refresh o cambio password.
 */
@Entity
@Table(name = "issued_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;
}
