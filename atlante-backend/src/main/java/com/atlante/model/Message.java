package com.atlante.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "ix_msg_conv_sent", columnList = "conversation_id, sent_at"),
        @Index(name = "ix_msg_conv_rcpt_status", columnList = "conversation_id, recipient_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(nullable = false, length = 2000)
    private String text;

    /** Connessione condivisa nel messaggio (facoltativa, senza vincolo: può essere eliminata). */
    @Column(name = "connection_id")
    private Long connectionId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    public Message(Conversation conversation, User sender, User recipient, String text) {
        this.conversation = conversation;
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
        this.sentAt = Instant.now();
        this.status = MessageStatus.SENT;
    }

    public Message(Conversation conversation, User sender, User recipient, String text, Long connectionId) {
        this(conversation, sender, recipient, text);
        this.connectionId = connectionId;
    }

    public void markDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
            this.deliveredAt = Instant.now();
        }
    }
}
