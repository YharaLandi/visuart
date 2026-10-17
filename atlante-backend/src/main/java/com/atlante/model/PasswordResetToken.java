package com.atlante.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Token monouso per il recupero password. Nel database c'è solo l'hash SHA-256, mai il token in chiaro. */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }
}
