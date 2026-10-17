import { Client } from '@stomp/stompjs';
import { currentToken, wsUrl } from './client';

/**
 * Sessione "di sfondo" sempre attiva da loggati: notifiche di like e commenti e
 * avviso di attività in chat (per aggiornare il badge dei non letti in ogni pagina).
 */
export function connectNotifications(onNotification, onChatActivity, onMessage) {
  const client = new Client({
    brokerURL: wsUrl(),
    connectHeaders: { Authorization: `Bearer ${currentToken()}` },
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe('/user/queue/notifications', (f) => onNotification(JSON.parse(f.body)));
      client.subscribe('/user/queue/messages', (f) => {
        onChatActivity?.();
        onMessage?.(JSON.parse(f.body));
      });
      client.subscribe('/user/queue/updates', (f) => {
        if (JSON.parse(f.body).type === 'READ') onChatActivity?.();
      });
    },
  });
  client.activate();
  return () => client.deactivate();
}

/**
 * Apre la sessione STOMP autenticata col JWT e sottoscrive le code personali della chat.
 * I frame non validi arrivano su onError.
 */
export function connectChat({ onMessage, onUpdate, onError, onStatus }) {
  const client = new Client({
    brokerURL: wsUrl(),
    connectHeaders: { Authorization: `Bearer ${currentToken()}` },
    reconnectDelay: 3000,
    onConnect: () => {
      onStatus?.(true);
      client.subscribe('/user/queue/messages', (f) => onMessage?.(JSON.parse(f.body)));
      client.subscribe('/user/queue/updates', (f) => onUpdate?.(JSON.parse(f.body)));
      client.subscribe('/user/queue/errors', (f) => onError?.(JSON.parse(f.body).error));
    },
    onWebSocketClose: () => onStatus?.(false),
    onStompError: (frame) => onError?.(frame.headers?.message || 'Errore di connessione chat'),
  });
  client.activate();

  const publish = (destination, body) => {
    if (client.connected) {
      client.publish({ destination, body: JSON.stringify(body) });
    }
  };

  return {
    sendMessage: (recipient, text, connectionId) =>
      publish('/app/chat.send', { recipient, text, connectionId, tempId: crypto.randomUUID() }),
    markRead: (conversationId) => publish('/app/chat.read', { conversationId }),
    typing: (recipient) => publish('/app/chat.typing', { recipient }),
    disconnect: () => client.deactivate(),
  };
}
