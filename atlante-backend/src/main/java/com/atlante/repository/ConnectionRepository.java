package com.atlante.repository;

import com.atlante.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Connection> {

    List<Connection> findByCategorySlug(String slug);

    List<Connection> findByAuthor(com.atlante.model.User author);

    List<Connection> findAllByOrderByViewCountDesc();

    List<Connection> findByCategorySlugOrderByViewCountDesc(String slug);

    @Modifying
    @Query("update Connection c set c.viewCount = c.viewCount + 1 where c.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Query("select c from Connection c where c.source.id = :workId or c.target.id = :workId")
    List<Connection> findAllTouching(@Param("workId") Long workId);

    @Query("""
            select c from Connection c
            where (c.source.id in :workIds or c.target.id in :workIds)
            and (:slug is null or c.category.slug = :slug)
            """)
    List<Connection> findTouchingAny(@Param("workIds") List<Long> workIds, @Param("slug") String slug);
}
