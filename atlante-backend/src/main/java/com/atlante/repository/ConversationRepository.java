package com.atlante.repository;

import com.atlante.model.Conversation;
import com.atlante.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUserOneAndUserTwo(User userOne, User userTwo);

    @Query("select c from Conversation c where c.userOne.username = :username or c.userTwo.username = :username")
    List<Conversation> ofUser(@Param("username") String username);

    @Query("""
            select case when c.userOne.username = :username then c.userTwo.username else c.userOne.username end
            from Conversation c
            where c.userOne.username = :username or c.userTwo.username = :username
            """)
    List<String> contactUsernames(@Param("username") String username);
}
