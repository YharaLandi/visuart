package com.atlante.service;

import java.time.Duration;
import java.time.Instant;

/**
 * Punteggio di popolarità di una connessione: decide chi va in cima a "Più viste" e chi
 * finisce in cartellone nella home.
 *
 * <pre>
 *   interazioni = 3 · mi piace + 5 · commenti + 2 · log10(1 + visualizzazioni)
 *   punteggio   = interazioni / (giorni dalla pubblicazione + 2) ^ 0.25
 * </pre>
 *
 * I commenti pesano più dei mi piace perché costano più fatica; le visualizzazioni entrano in
 * scala logaritmica, così una connessione molto aperta ma mai discussa non scavalca quelle che
 * fanno parlare. Il decadimento nel tempo è dolce (esponente 0,25): una novità con qualche
 * interazione può salire, ma non basta essere recenti per stare in alto.
 */
public final class PopularityScore {

    static final double LIKE_WEIGHT = 3;
    static final double COMMENT_WEIGHT = 5;
    static final double VIEW_WEIGHT = 2;
    static final double GRAVITY = 0.25;

    private PopularityScore() {
    }

    public static double of(long likes, long comments, long views, Instant createdAt, Instant now) {
        double interactions = LIKE_WEIGHT * likes + COMMENT_WEIGHT * comments
                + VIEW_WEIGHT * Math.log10(1 + Math.max(0, views));
        double days = createdAt == null ? 0 : Math.max(0, Duration.between(createdAt, now).toHours() / 24.0);
        return interactions / Math.pow(days + 2, GRAVITY);
    }
}
