package com.atlante.service;

import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.Role;
import com.atlante.model.User;
import com.atlante.repository.RoleRepository;
import com.atlante.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ruoli come dati: i tre di sistema esistono sempre, gli altri li crea l'admin. Ogni utente ha
 * almeno il ruolo USER; ADMIN e CURATOR si aggiungono sopra.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private static final Pattern NAME = Pattern.compile("^[A-Z][A-Z_]{2,29}$");

    public static final Map<String, String> SYSTEM = Map.of(
            Role.ADMIN, "Gestisce utenti, ruoli e contenuti di tutti.",
            Role.CURATOR, "Scrive connessioni e cura il reparto fantasma e le curiosità.",
            Role.USER, "Legge, commenta, salva e scrive le proprie connessioni.");

    private final RoleRepository roles;
    private final UserRepository users;
    private final TokenService tokens;

    public record RoleView(Long id, String name, String description, boolean system, long users) {
    }

    /** Crea i ruoli di sistema se mancano: si chiama all'avvio, prima di ogni altro dato. */
    public void ensureSystemRoles() {
        SYSTEM.forEach((name, description) -> {
            if (!roles.existsByName(name)) roles.save(new Role(name, description, true));
        });
    }

    public Role get(String name) {
        return roles.findByName(name).orElseThrow(() -> new ResourceNotFoundException("Ruolo non trovato: " + name));
    }

    /** Insieme di ruoli per un nuovo utente: USER più quelli indicati. */
    public Set<Role> setOf(String... names) {
        Set<Role> set = new HashSet<>();
        set.add(get(Role.USER));
        for (String n : names) set.add(get(n));
        return set;
    }

    @Transactional(readOnly = true)
    public List<RoleView> list() {
        return roles.findAll().stream()
                .sorted((a, b) -> a.isSystem() == b.isSystem() ? a.getName().compareTo(b.getName()) : a.isSystem() ? -1 : 1)
                .map(r -> new RoleView(r.getId(), r.getName(), r.getDescription(), r.isSystem(), roles.countUsers(r.getId())))
                .toList();
    }

    public RoleView create(String rawName, String description) {
        String name = normalize(rawName);
        if (roles.existsByName(name)) throw new IllegalArgumentException("Esiste già un ruolo " + name);
        Role r = roles.save(new Role(name, clean(description), false));
        return new RoleView(r.getId(), r.getName(), r.getDescription(), false, 0);
    }

    public RoleView update(Long id, String rawName, String description) {
        Role r = find(id);
        if (r.isSystem()) {
            // dei ruoli di sistema si può cambiare solo la descrizione
            if (rawName != null && !normalize(rawName).equals(r.getName())) {
                throw new IllegalArgumentException("I ruoli di sistema non si rinominano");
            }
        } else if (rawName != null) {
            String name = normalize(rawName);
            if (!name.equals(r.getName()) && roles.existsByName(name)) {
                throw new IllegalArgumentException("Esiste già un ruolo " + name);
            }
            r.setName(name);
        }
        r.setDescription(clean(description));
        return new RoleView(r.getId(), r.getName(), r.getDescription(), r.isSystem(), roles.countUsers(r.getId()));
    }

    /** Si elimina un ruolo creato dall'admin; chi lo aveva lo perde, il resto dei suoi ruoli resta. */
    public void delete(Long id) {
        Role r = find(id);
        if (r.isSystem()) throw new IllegalArgumentException("I ruoli di sistema non si eliminano");
        for (User u : users.findAll()) {
            if (u.getRoles().remove(r)) {
                users.save(u);
                tokens.revokeAll(u);
            }
        }
        roles.delete(r);
    }

    /**
     * Assegna all'utente esattamente questi ruoli (USER resta sempre). Le sessioni aperte si chiudono:
     * al prossimo accesso i permessi sono quelli nuovi.
     */
    public User assign(User user, Collection<String> names) {
        Set<Role> next = new HashSet<>();
        next.add(get(Role.USER));
        for (String n : names) next.add(get(normalize(n)));
        user.getRoles().clear();
        user.getRoles().addAll(next);
        tokens.revokeAll(user);
        return users.save(user);
    }

    private Role find(Long id) {
        return roles.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ruolo non trovato: " + id));
    }

    private static String normalize(String raw) {
        String name = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Nome del ruolo non valido: da 3 a 30 lettere maiuscole o trattini bassi, es. MODERATORE");
        }
        return name;
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
