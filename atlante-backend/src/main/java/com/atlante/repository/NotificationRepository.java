package com.atlante.repository;

import com.atlante.model.Notification;
import com.atlante.model.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Limit limit);

    long countByRecipientAndReadFalse(User recipient);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipient = :user and n.read = false")
    int markAllRead(@Param("user") User user);

    void deleteByRecipient(User recipient);

    void deleteByConnectionId(Long connectionId);
}
