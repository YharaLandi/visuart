package com.atlante.model;

import jakarta.persistence.*;

/**
 * Ruolo salvato nel database (tabella {@code roles}), collegato agli utenti in molti-a-molti
 * tramite {@code user_roles}. I tre ruoli di sistema esistono sempre e non si possono eliminare
 * né rinominare; l'admin può crearne altri e assegnarli. Spring Security li vede come
 * autorità {@code ROLE_<NOME>}.
 */
@Entity
@Table(name = "roles")
public class Role {

    public static final String ADMIN = "ADMIN";
    public static final String CURATOR = "CURATOR";
    public static final String USER = "USER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome tecnico in maiuscolo (es. CURATOR): è quello che finisce nell'autorità ROLE_... */
    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(length = 200)
    private String description;

    /** I ruoli di sistema reggono i permessi dell'applicazione: non si eliminano né si rinominano. */
    @Column(name = "system_role", nullable = false)
    private boolean system;

    public Role() {
    }

    public Role(String name, String description, boolean system) {
        this.name = name;
        this.description = description;
        this.system = system;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isSystem() { return system; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Role r && name != null && name.equals(r.name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }
}
