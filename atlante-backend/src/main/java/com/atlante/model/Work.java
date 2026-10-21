package com.atlante.model;

import jakarta.persistence.*;

@Entity
@Table(name = "works", uniqueConstraints = @UniqueConstraint(columnNames = {"external_source", "external_id"}))
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WorkType type;

    @Column(nullable = false)
    private String title;

    private String creator;

    @Column(name = "release_year")
    private Integer year;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /** Solo per i film: un fotogramma (la locandina resta in imageUrl e si vede nella scheda). */
    @Column(name = "still_url")
    private String stillUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_source", length = 20)
    private ExternalSource externalSource = ExternalSource.MANUAL;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String palette;

    public Work() {
    }

    public Work(WorkType type, String title, String creator, Integer year) {
        this.type = type;
        this.title = title;
        this.creator = creator;
        this.year = year;
    }

    public Long getId() { return id; }
    public WorkType getType() { return type; }
    public void setType(WorkType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getImageUrl() { return imageUrl; }
    public String getStillUrl() { return stillUrl; }
    public void setStillUrl(String stillUrl) { this.stillUrl = stillUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public ExternalSource getExternalSource() { return externalSource; }
    public void setExternalSource(ExternalSource externalSource) { this.externalSource = externalSource; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPalette() { return palette; }
    public void setPalette(String palette) { this.palette = palette; }
}
