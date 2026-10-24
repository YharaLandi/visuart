package com.atlante.service;

import com.atlante.dto.ChatDtos.ChatUpdate;
import com.atlante.dto.ChatDtos.ConversationSummary;
import com.atlante.dto.ChatDtos.MessageResponse;
import com.atlante.dto.ChatDtos.MessagesPage;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.exception.UnauthorizedException;
import com.atlante.model.Conversation;
import com.atlante.model.Message;
import com.atlante.model.MessageStatus;
import com.atlante.model.User;
import com.atlante.repository.ConversationRepository;
import com.atlante.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Limit;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Messaggistica 1-a-1 in tempo reale. Il salvataggio passa dal database, la
 * consegna dai destinatari STOMP /user/queue/messages e /user/queue/updates.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    public static final String MESSAGES = "/queue/messages";
    public static final String UPDATES = "/queue/updates";
    private static final int PAGE_SIZE = 30;
    private static final MessagesPage EMPTY = new MessagesPage(null, List.of(), 0, false, null);

    private final MessageRepository messages;
    private final ConversationRepository conversations;
    private final UserService userService;
    private final SimpMessagingTemplate template;
    private final SimpUserRegistry registry;
    private final com.atlante.repository.ConnectionRepository connections;

    public ChatService(MessageRepository messages,
                       ConversationRepository conversations,
                       com.atlante.repository.ConnectionRepository connections,
                       UserService userService,
                       SimpMessagingTemplate template,
                       SimpUserRegistry registry) {
        this.messages = messages;
        this.conversations = conversations;
        this.userService = userService;
        this.template = template;
        this.registry = registry;
        this.connections = connections;
    }

    public MessageResponse send(String senderUsername, String recipientUsername, String text, String tempId) {
        return send(senderUsername, recipientUsername, text, tempId, null);
    }

    public MessageResponse send(String senderUsername, String recipientUsername, String text, String tempId,
                                Long connectionId) {
        if (connectionId != null && !connections.existsById(connectionId)) {
            throw new ResourceNotFoundException("Connessione non trovata: " + connectionId);
        }
        User sender = userService.findByUsername(senderUsername);
        User recipient = userService.findByUsername(recipientUsername);

        Conversation conversation = findOrCreate(sender, recipient);
        Message saved = messages.save(new Message(conversation, sender, recipient, text.trim(), connectionId));

        if (registry.getUser(recipientUsername) != null) {
            saved.markDelivered();
            saved = messages.save(saved);
        }

        template.convertAndSendToUser(recipientUsername, MESSAGES,
                MessageResponse.of(saved, senderUsername, recipientUsername, null));
        // il mittente riceve l'eco con il tempId, per sostituire il messaggio "in invio" nella UI
        template.convertAndSendToUser(senderUsername, MESSAGES,
                MessageResponse.of(saved, senderUsername, recipientUsername, tempId));

        log.info("messaggio {} conv={} {} -> {} ({})", saved.getId(), conversation.getId(),
                senderUsername, recipientUsername, saved.getStatus());
        return MessageResponse.of(saved, senderUsername, recipientUsername, null);
    }

    public void typing(String whoUsername, String toUsername) {
        User who = userService.findByUsername(whoUsername);
        User to = userService.findByUsername(toUsername);
        find(who, to).ifPresent(conv ->
                template.convertAndSendToUser(toUsername, UPDATES, ChatUpdate.typing(conv.getId(), whoUsername)));
    }

    @Transactional
    public void markRead(Long conversationId, String username) {
        Conversation conv = requireParticipant(conversationId, username);

        List<Long> ids = messages.unreadIds(conversationId, username);
        if (ids.isEmpty()) {
            return;
        }
        messages.markRead(conversationId, username, MessageStatus.READ);

        String counterpart = conv.counterpartOf(username).getUsername();
        ChatUpdate update = ChatUpdate.read(conversationId, username, ids);
        template.convertAndSendToUser(counterpart, UPDATES, update);
        template.convertAndSendToUser(username, UPDATES, update);

        log.info("letti {} messaggi conv={} da {}", ids.size(), conversationId, username);
    }

    @Transactional
    public void markDelivered(Long conversationId, String username) {
        requireParticipant(conversationId, username);
        messages.markDelivered(conversationId, username, Instant.now());
    }

    @Transactional(readOnly = true)
    public MessagesPage history(String username, String withUsername) {
        return history(username, withUsername, null);
    }

    /** Ultimi messaggi o, con before, quelli precedenti a un id ("carica precedenti"). */
    @Transactional(readOnly = true)
    public MessagesPage history(String username, String withUsername, Long before) {
        User user = userService.findByUsername(username);
        User with = userService.findByUsername(withUsername);

        Conversation conv = find(user, with).orElse(null);
        if (conv == null) {
            return EMPTY;
        }
        // se ne chiede uno in più: se arriva, ci sono ancora messaggi più vecchi
        List<Message> found = before == null
                ? messages.latest(conv.getId(), Limit.of(PAGE_SIZE + 1))
                : messages.latestBefore(conv.getId(), before, Limit.of(PAGE_SIZE + 1));
        boolean hasMore = found.size() > PAGE_SIZE;
        List<MessageResponse> page = new ArrayList<>(found.stream().limit(PAGE_SIZE)
                .map(MessageResponse::of)
                .toList());
        page.sort(Comparator.comparing(MessageResponse::sentAt).thenComparing(MessageResponse::id));
        List<Long> shared = messages.sharedConnectionIds(conv.getId(), Limit.of(1));
        return new MessagesPage(conv.getId(), page, messages.unread(conv.getId(), username), hasMore,
                shared.isEmpty() ? null : shared.getFirst());
    }

    @Transactional(readOnly = true)
    public long unreadTotal(String username) {
        return messages.unreadTotal(username);
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> summaries(String username) {
        return conversations.ofUser(username).stream()
                .map(c -> {
                    List<Message> last = messages.latest(c.getId(), Limit.of(1));
                    User other = c.counterpartOf(username);
                    String counterpart = other.getUsername();
                    return new ConversationSummary(
                            c.getId(),
                            counterpart,
                            other.getDisplayName(),
                            other.getRoleName(),
                            messages.unread(c.getId(), username),
                            last.isEmpty() ? null : last.getFirst().getText(),
                            last.isEmpty() ? null : last.getFirst().getSentAt(),
                            registry.getUser(counterpart) != null);
                })
                .sorted(Comparator.comparing(ConversationSummary::lastAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onConnect(SessionConnectedEvent event) {
        broadcastPresence(event.getUser() == null ? null : event.getUser().getName());
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onDisconnect(SessionDisconnectEvent event) {
        broadcastPresence(event.getUser() == null ? null : event.getUser().getName());
    }

    private void broadcastPresence(String username) {
        if (username == null) {
            return;
        }
        conversations.contactUsernames(username).forEach(contact ->
                template.convertAndSendToUser(contact, UPDATES, ChatUpdate.presence(username)));
    }

    public Optional<Conversation> find(User a, User b) {
        if (a.getUsername().equals(b.getUsername())) {
            throw new IllegalArgumentException("recipient: non puoi scrivere a te stesso");
        }
        return conversations.findByUserOneAndUserTwo(Conversation.first(a, b), Conversation.second(a, b));
    }

    @Transactional
    public Conversation findOrCreate(User a, User b) {
        return find(a, b).orElseGet(() -> conversations.save(Conversation.between(a, b)));
    }

    /** Verifica che la conversazione esista e che l'utente ne faccia parte. */
    @Transactional(readOnly = true)
    public Conversation requireParticipant(Long conversationId, String username) {
        Conversation conv = conversations.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversazione non trovata: " + conversationId));
        if (!conv.includes(username)) {
            throw new UnauthorizedException("Non sei un partecipante di questa conversazione");
        }
        return conv;
    }
}
