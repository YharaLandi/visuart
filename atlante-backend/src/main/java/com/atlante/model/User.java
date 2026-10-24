package com.atlante.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 120)
    private String displayName;

    /** Ruoli dell'utente (tabella user_roles): letti a ogni richiesta, quindi un cambio vale subito. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /** Breve frase personale mostrata nel profilo. */
    @Column(length = 300)
    private String bio;

    /** Nome del file della foto profilo nella cartella avatars; null = si mostra l'iniziale. */
    @JsonIgnore
    @Column(name = "avatar_file", length = 120)
    private String avatarFile;

    @Column(name = "created_at")
    @Builder.Default
    private java.time.Instant createdAt = java.time.Instant.now();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName())).toList();
    }

    public boolean hasRole(String name) {
        return roles.stream().anyMatch(r -> r.getName().equals(name));
    }

    /**
     * Il ruolo principale, quello mostrato accanto al nome: ADMIN, poi CURATOR, poi un ruolo creato
     * dall'admin, infine USER.
     */
    public String getRoleName() {
        if (hasRole(Role.ADMIN)) return Role.ADMIN;
        if (hasRole(Role.CURATOR)) return Role.CURATOR;
        return roles.stream().map(Role::getName).filter(n -> !n.equals(Role.USER)).sorted().findFirst()
                .orElse(Role.USER);
    }

    public List<String> getRoleNames() {
        return roles.stream().map(Role::getName).sorted().toList();
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
