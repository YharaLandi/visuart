package com.atlante.repository;

import com.atlante.model.HorrorOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HorrorOriginRepository extends JpaRepository<HorrorOrigin, Long> {

    @Query("select o from HorrorOrigin o join fetch o.film f order by f.year asc")
    List<HorrorOrigin> findAllWithFilm();

    Optional<HorrorOrigin> findByFilmId(Long filmId);
}
