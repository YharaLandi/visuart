package com.atlante.service;

import com.atlante.dto.Dtos.NotificationResponse;
import com.atlante.model.Connection;
import com.atlante.model.Notification;
import com.atlante.model.User;
import com.atlante.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Notifiche "qualcuno ha messo like / commentato la tua connessione".
 * Si salvano nella transazione dell'azione; la spinta WebSocket parte solo
 * dopo il commit, così non si annuncia mai qualcosa che poi è stato annullato.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String QUEUE = "/queue/notifications";

    /** Evento interno: notifica salvata, da consegnare al destinatario. */
    public record Created(String recipientUsername, NotificationResponse notification) {
    }

    private final NotificationRepository repository;
    private final SimpMessagingTemplate template;
    private final ApplicationEventPublisher events;

    @Transactional
    public void notify(User actor, Connection connection, Notification.Type type, String preview) {
        User recipient = connection.getAuthor();
        // niente notifiche per le azioni sulle proprie connessioni o su quelle senza autore
        if (recipient == null || recipient.getId().equals(actor.getId())) {
            return;
        }
        Notification saved = repository.save(Notification.builder()
                .recipient(recipient)
                .actorUsername(actor.getUsername())
                .type(type)
                .connectionId(connection.getId())
                .preview(preview != null && preview.length() > 300 ? preview.substring(0, 297) + "…" : preview)
                .build());
        events.publishEvent(new Created(recipient.getUsername(), NotificationResponse.from(saved)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void push(Created event) {
        template.convertAndSendToUser(event.recipientUsername(), QUEUE, event.notification());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> latest(User user) {
        return repository.findByRecipientOrderByCreatedAtDesc(user, Limit.of(30)).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unread(User user) {
        return repository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public void markAllRead(User user) {
        repository.markAllRead(user);
    }
}
