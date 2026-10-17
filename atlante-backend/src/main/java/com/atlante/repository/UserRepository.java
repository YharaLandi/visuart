package com.atlante.repository;

import com.atlante.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Ricerca per la chat: username o nome visualizzato, escluso chi cerca. */
    @org.springframework.data.jpa.repository.Query("""
            select u from User u
            where u.username <> :self
            and (lower(u.username) like lower(concat('%', :q, '%'))
                 or lower(u.displayName) like lower(concat('%', :q, '%')))
            order by u.username
            """)
    java.util.List<User> searchOthers(@org.springframework.data.repository.query.Param("q") String q,
                                      @org.springframework.data.repository.query.Param("self") String self,
                                      org.springframework.data.domain.Limit limit);

    org.springframework.data.domain.Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, org.springframework.data.domain.Pageable pageable);
}
