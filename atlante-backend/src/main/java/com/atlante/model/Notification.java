package com.atlante.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = @Index(name = "ix_notif_recipient", columnList = "recipient_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    public enum Type { LIKE, COMMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /** Username di chi ha compiuto l'azione (salvato come testo: la notifica sopravvive a modifiche). */
    @Column(name = "actor_username", nullable = false, length = 60)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    /** Connessione a cui si riferisce; senza vincolo, così eliminarla non blocca nulla. */
    @Column(name = "connection_id")
    private Long connectionId;

    @Column(length = 300)
    private String preview;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
