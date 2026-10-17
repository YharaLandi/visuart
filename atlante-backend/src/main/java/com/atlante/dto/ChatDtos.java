package com.atlante.dto;

import com.atlante.model.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    /** Frame STOMP in ingresso su /app/chat.send. */
    public record SendMessage(String recipient, String text, String tempId, Long connectionId) {

        public SendMessage {
            if (recipient == null || recipient.isBlank()) {
                throw new IllegalArgumentException("recipient: non può essere vuoto");
            }
            // un messaggio può essere solo una connessione condivisa, senza testo
            if ((text == null || text.isBlank()) && connectionId == null) {
                throw new IllegalArgumentException("text: non può essere vuoto");
            }
            if (text == null) {
                text = "";
            }
            if (text.length() > 2000) {
                throw new IllegalArgumentException("text: al massimo 2000 caratteri");
            }
        }
    }

    /** Frame STOMP in ingresso su /app/chat.typing. */
    public record Typing(String recipient) {

        public Typing {
            if (recipient == null || recipient.isBlank()) {
                throw new IllegalArgumentException("recipient: non può essere vuoto");
            }
        }
    }

    /** Frame STOMP in ingresso su /app/chat.read. */
    public record MarkRead(Long conversationId) {

        public MarkRead {
            if (conversationId == null) {
                throw new IllegalArgumentException("conversationId: obbligatorio");
            }
        }
    }

    public record MessageResponse(Long id, Long conversationId, String sender, String recipient,
                                  String text, Long connectionId, Instant sentAt, Instant deliveredAt,
                                  String status, String tempId) {

        public static MessageResponse of(Message m, String sender, String recipient, String tempId) {
            return new MessageResponse(m.getId(), m.getConversation().getId(), sender, recipient,
                    m.getText(), m.getConnectionId(), m.getSentAt(), m.getDeliveredAt(),
                    m.getStatus().name(), tempId);
        }

        /** Da usare solo dentro una transazione, con le associazioni lazy ancora caricabili. */
        public static MessageResponse of(Message m) {
            return of(m, m.getSender().getUsername(), m.getRecipient().getUsername(), null);
        }
    }

    /** Eventi laterali verso /user/queue/updates: lettura, "sta scrivendo", presenza. */
    public record ChatUpdate(String type, Long conversationId, String user, List<Long> messageIds) {

        public static ChatUpdate read(Long conversationId, String by, List<Long> messageIds) {
            return new ChatUpdate("READ", conversationId, by, messageIds);
        }

        public static ChatUpdate typing(Long conversationId, String who) {
            return new ChatUpdate("TYPING", conversationId, who, List.of());
        }

        public static ChatUpdate presence(String user) {
            return new ChatUpdate("PRESENCE", null, user, List.of());
        }
    }

    public record ConversationSummary(Long conversationId, String counterpart, String counterpartName,
                                      String counterpartRole, long unread,
                                      String lastText, Instant lastAt, boolean online) {
    }

    /** lastSharedConnectionId: l'ultima connessione condivisa, mostrata come "state parlando di". */
    public record MessagesPage(Long conversationId, List<MessageResponse> messages, long unread,
                               boolean hasMore, Long lastSharedConnectionId) {
    }

    public record NewChatRequest(@NotBlank String recipientUsername) {
    }

    public record SuggestionRequest(@NotNull Long conversationId) {
    }

    public record SuggestionResponse(String suggestion) {
    }
}
