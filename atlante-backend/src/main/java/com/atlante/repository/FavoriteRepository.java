package com.atlante.repository;

import com.atlante.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Favorite> findByUserIdAndWorkId(Long userId, Long workId);

    boolean existsByUserIdAndWorkId(Long userId, Long workId);

    List<Favorite> findByWorkId(Long workId);
}
