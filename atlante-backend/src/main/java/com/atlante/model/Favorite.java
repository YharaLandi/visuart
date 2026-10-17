package com.atlante.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "favorites", uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_id", "work_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id")
    private Work work;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Favorite() {
    }

    public Favorite(User user, Work work) {
        this.user = user;
        this.work = work;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Work getWork() { return work; }
    public Instant getCreatedAt() { return createdAt; }
}
