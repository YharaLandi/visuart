package com.atlante.repository;

import com.atlante.model.Connection;
import com.atlante.model.Like;
import com.atlante.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndConnection(User user, Connection connection);

    List<Like> findByConnection(Connection connection);

    List<Like> findByUser(User user);

    long countByConnectionId(Long connectionId);

    @Query("select count(l) from Like l where l.connection.author = :author")
    long countReceivedBy(@Param("author") User author);

    @Query("select l.connection.id, count(l) from Like l where l.connection.id in :ids group by l.connection.id")
    List<Object[]> countByConnectionIds(@Param("ids") Collection<Long> ids);

    @Query("select l.connection.id from Like l where l.user.username = :username and l.connection.id in :ids")
    List<Long> likedConnectionIds(@Param("username") String username, @Param("ids") Collection<Long> ids);
}
