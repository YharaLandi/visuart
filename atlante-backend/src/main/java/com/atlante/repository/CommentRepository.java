package com.atlante.repository;

import com.atlante.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByConnectionIdOrderByCreatedAtAsc(Long connectionId);

    List<Comment> findByAuthor(com.atlante.model.User author);

    long countByAuthor(com.atlante.model.User author);

    @org.springframework.data.jpa.repository.Query("select c.connection.id, count(c) from Comment c where c.connection.id in :ids group by c.connection.id")
    List<Object[]> countByConnectionIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}
