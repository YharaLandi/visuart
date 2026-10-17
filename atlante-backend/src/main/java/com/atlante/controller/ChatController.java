package com.atlante.controller;

import com.atlante.dto.ChatDtos.MarkRead;
import com.atlante.dto.ChatDtos.SendMessage;
import com.atlante.dto.ChatDtos.Typing;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.exception.UnauthorizedException;
import com.atlante.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/** Endpoint STOMP: il client invia verso /app/chat.*, le risposte arrivano su /user/queue/*. */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void send(SendMessage request, Principal sender) {
        if (sender == null) {
            log.warn("messaggio da una sessione anonima: scartato");
            return;
        }
        chatService.send(sender.getName(), request.recipient(), request.text(), request.tempId(),
                request.connectionId());
    }

    @MessageMapping("/chat.read")
    public void read(MarkRead request, Principal user) {
        if (user == null) return;
        chatService.markRead(request.conversationId(), user.getName());
    }

    @MessageMapping("/chat.typing")
    public void typing(Typing request, Principal user) {
        if (user == null) return;
        chatService.typing(user.getName(), request.recipient());
    }

    /** Gli errori di validazione tornano solo a chi li ha provocati, su /user/queue/errors. */
    @MessageExceptionHandler({IllegalArgumentException.class, ResourceNotFoundException.class,
            UnauthorizedException.class})
    @SendToUser("/queue/errors")
    public Map<String, String> onError(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }
}
