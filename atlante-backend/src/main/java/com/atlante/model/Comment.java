package com.atlante.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id")
    private Connection connection;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "edited_at")
    private Instant editedAt;

    public Comment() {
    }

    public Comment(Connection connection, User author, String text) {
        this.connection = connection;
        this.author = author;
        this.text = text;
    }

    public Long getId() { return id; }
    public Connection getConnection() { return connection; }
    public User getAuthor() { return author; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEditedAt() { return editedAt; }

    public void edit(String newText) {
        this.text = newText;
        this.editedAt = Instant.now();
    }
}
