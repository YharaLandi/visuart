package com.atlante.repository;

import com.atlante.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    /** Quanti utenti hanno il ruolo: serve per mostrarlo nella gestione e per impedire cancellazioni distruttive. */
    @Query(value = "select count(*) from user_roles where role_id = :roleId", nativeQuery = true)
    long countUsers(Long roleId);
}
