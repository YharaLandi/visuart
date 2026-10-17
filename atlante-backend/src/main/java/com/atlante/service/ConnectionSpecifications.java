package com.atlante.service;

import com.atlante.model.Connection;
import com.atlante.model.WorkType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Filtri combinabili per la ricerca paginata delle connessioni. */
public final class ConnectionSpecifications {

    private ConnectionSpecifications() {
    }

    public static Specification<Connection> inCategory(String slug) {
        return (root, query, cb) -> slug == null || slug.isBlank()
                ? null
                : cb.equal(root.join("category").get("slug"), slug);
    }

    /** Testo libero su titoli e autori delle due opere e sulla nota critica. */
    public static Specification<Connection> matches(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            Join<Object, Object> source = root.join("source");
            Join<Object, Object> target = root.join("target");
            return cb.or(
                    cb.like(cb.lower(source.get("title")), like),
                    cb.like(cb.lower(target.get("title")), like),
                    cb.like(cb.lower(source.get("creator")), like),
                    cb.like(cb.lower(target.get("creator")), like),
                    cb.like(cb.lower(root.get("criticalNote")), like));
        };
    }

    /** Connessioni che coinvolgono almeno un'opera del tipo indicato. */
    public static Specification<Connection> involving(WorkType type) {
        return (root, query, cb) -> type == null
                ? null
                : cb.or(cb.equal(root.join("source").get("type"), type),
                        cb.equal(root.join("target").get("type"), type));
    }

    public static Specification<Connection> byAuthor(String username) {
        return (root, query, cb) -> username == null || username.isBlank()
                ? null
                : cb.equal(root.join("author").get("username"), username);
    }
}
