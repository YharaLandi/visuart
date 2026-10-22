package com.atlante.model;

import jakarta.persistence.*;

/**
 * Reparto fantasma: da dove viene un film horror (una leggenda, un mito, un romanzo, un fatto di
 * cronaca). Per i casi di true crime non si racconta nulla: restano solo il nome del caso, l'anno
 * e un link a Wikipedia, per rispetto delle persone coinvolte.
 */
@Entity
@Table(name = "horror_origins")
public class HorrorOrigin {

    /** Tipi di origine; il tipo è una stringa per restare portabile tra H2 e PostgreSQL. */
    public static final java.util.List<String> TYPES =
            java.util.List.of("FOLKLORE", "MITO", "LEGGENDA", "LETTERATURA", "STORIA", "CRONACA", "TRUE_CRIME");
    public static final String TRUE_CRIME = "TRUE_CRIME";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false, unique = true)
    private Work film;

    @Column(name = "origin_type", nullable = false, length = 20)
    private String type;

    /** Il nome della fonte: "La leggenda di Bloody Mary", "Il caso del 1949"… */
    @Column(name = "origin_title", nullable = false, length = 160)
    private String originTitle;

    @Column(name = "origin_year")
    private Integer originYear;

    /** Due o tre righe di curiosità; sempre vuoto per il true crime. */
    @Column(length = 500)
    private String trivia;

    @Column(name = "wikipedia_url", nullable = false, length = 300)
    private String wikipediaUrl;

    public HorrorOrigin() {
    }

    public HorrorOrigin(Work film, String type, String originTitle, Integer originYear, String trivia, String wikipediaUrl) {
        this.film = film;
        this.type = type;
        this.originTitle = originTitle;
        this.originYear = originYear;
        this.trivia = trivia;
        this.wikipediaUrl = wikipediaUrl;
    }

    public Long getId() { return id; }
    public Work getFilm() { return film; }
    public String getType() { return type; }
    public String getOriginTitle() { return originTitle; }
    public Integer getOriginYear() { return originYear; }
    public String getTrivia() { return trivia; }
    public String getWikipediaUrl() { return wikipediaUrl; }
    public void setType(String type) { this.type = type; }
    public void setOriginTitle(String originTitle) { this.originTitle = originTitle; }
    public void setOriginYear(Integer originYear) { this.originYear = originYear; }
    public void setTrivia(String trivia) { this.trivia = trivia; }
    public void setWikipediaUrl(String wikipediaUrl) { this.wikipediaUrl = wikipediaUrl; }
}
