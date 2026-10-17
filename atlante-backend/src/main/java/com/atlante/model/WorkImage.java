package com.atlante.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "work_images")
public class WorkImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id")
    private Work work;

    @Column(name = "url_original", columnDefinition = "TEXT")
    private String urlOriginal;

    @Column(name = "path_local", columnDefinition = "TEXT")
    private String pathLocal;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    public WorkImage() {
    }

    public WorkImage(Work work, String urlOriginal, String pathLocal) {
        this.work = work;
        this.urlOriginal = urlOriginal;
        this.pathLocal = pathLocal;
        this.fetchedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Work getWork() { return work; }
    public String getUrlOriginal() { return urlOriginal; }
    public String getPathLocal() { return pathLocal; }
    public Instant getFetchedAt() { return fetchedAt; }
}
