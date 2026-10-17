package com.atlante.repository;

import com.atlante.model.Message;
import com.atlante.model.MessageStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Modifying
    @Query("delete from Message m where m.conversation.id in :conversations")
    void deleteByConversationIds(@Param("conversations") List<Long> conversations);

    @Query("select count(m) from Message m where m.sender.username = :username")
    long countSent(@Param("username") String username);

    @Query("select count(m) from Message m where m.recipient.username = :username")
    long countReceived(@Param("username") String username);

    @Query("select m from Message m where m.conversation.id = :conversation order by m.sentAt desc, m.id desc")
    List<Message> latest(@Param("conversation") Long conversation, Limit limit);

    @Query("select m from Message m where m.conversation.id = :conversation and m.id < :before order by m.sentAt desc, m.id desc")
    List<Message> latestBefore(@Param("conversation") Long conversation, @Param("before") Long before, Limit limit);

    @Query("select m.connectionId from Message m where m.conversation.id = :conversation and m.connectionId is not null order by m.sentAt desc, m.id desc")
    List<Long> sharedConnectionIds(@Param("conversation") Long conversation, Limit limit);

    @Query("select count(m) from Message m where m.recipient.username = :username and m.status <> 'READ'")
    long unreadTotal(@Param("username") String username);

    @Query("select count(m) from Message m where m.conversation.id = :conversation and m.recipient.username = :username and m.status <> 'READ'")
    long unread(@Param("conversation") Long conversation, @Param("username") String username);

    @Query("select m.id from Message m where m.conversation.id = :conversation and m.recipient.username = :username and m.status <> 'READ'")
    List<Long> unreadIds(@Param("conversation") Long conversation, @Param("username") String username);

    @Modifying
    @Query("update Message m set m.status = :status where m.conversation.id = :conversation and m.recipient.username = :username and m.status <> 'READ'")
    void markRead(@Param("conversation") Long conversation, @Param("username") String username,
                  @Param("status") MessageStatus status);

    @Modifying
    @Query("update Message m set m.status = 'DELIVERED', m.deliveredAt = :now where m.conversation.id = :conversation and m.recipient.username = :username and m.status = 'SENT'")
    void markDelivered(@Param("conversation") Long conversation, @Param("username") String username,
                       @Param("now") Instant now);
}
