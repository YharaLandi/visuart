package com.atlante.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "connections", uniqueConstraints =
        @UniqueConstraint(columnNames = {"source_work_id", "target_work_id", "category_id"}))
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_work_id")
    private Work source;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_work_id")
    private Work target;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "critical_note", nullable = false, columnDefinition = "TEXT")
    private String criticalNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    /**
     * Immagini scelte da chi scrive, solo per questa connessione: un fotogramma preciso del
     * film o un dettaglio del quadro. L'opera resta quella degli archivi (titolo, autore,
     * scheda); se mancano si usa l'immagine dell'opera.
     */
    @Column(name = "source_image_url")
    private String sourceImageUrl;

    @Column(name = "target_image_url")
    private String targetImageUrl;

    /** Palette scelte da chi scrive per questa connessione (esadecimali separati da virgola). */
    @Column(name = "source_palette", length = 120)
    private String sourcePalette;

    @Column(name = "target_palette", length = 120)
    private String targetPalette;

    public Connection() {
    }

    public Connection(Work source, Work target, Category category, String criticalNote, User author) {
        this.source = source;
        this.target = target;
        this.category = category;
        this.criticalNote = criticalNote;
        this.author = author;
    }

    public Long getId() { return id; }
    public Work getSource() { return source; }
    public void setSource(Work source) { this.source = source; }
    public Work getTarget() { return target; }
    public void setTarget(Work target) { this.target = target; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getCriticalNote() { return criticalNote; }
    public void setCriticalNote(String criticalNote) { this.criticalNote = criticalNote; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public Instant getCreatedAt() { return createdAt; }
    public long getViewCount() { return viewCount; }
    public void incrementViewCount() { this.viewCount++; }
    public String getSourceImageUrl() { return sourceImageUrl; }
    public void setSourceImageUrl(String url) { this.sourceImageUrl = url; }
    public String getTargetImageUrl() { return targetImageUrl; }
    public void setTargetImageUrl(String url) { this.targetImageUrl = url; }
    public String getSourcePalette() { return sourcePalette; }
    public void setSourcePalette(String palette) { this.sourcePalette = palette; }
    public String getTargetPalette() { return targetPalette; }
    public void setTargetPalette(String palette) { this.targetPalette = palette; }
}
