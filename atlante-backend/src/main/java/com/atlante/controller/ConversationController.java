package com.atlante.controller;

import com.atlante.dto.ChatDtos.ConversationSummary;
import com.atlante.dto.ChatDtos.MessagesPage;
import com.atlante.dto.ChatDtos.NewChatRequest;
import com.atlante.dto.ChatDtos.SuggestionRequest;
import com.atlante.dto.ChatDtos.SuggestionResponse;
import com.atlante.model.Conversation;
import com.atlante.model.User;
import com.atlante.service.ChatAssistantService;
import com.atlante.service.ChatService;
import com.atlante.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Parte REST della chat: elenco, cronologia, nuova conversazione e suggerimento AI. */
@RestController
@RequestMapping("/api/chat")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatService chatService;
    private final ChatAssistantService assistantService;
    private final UserService userService;

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationSummary create(@Valid @RequestBody NewChatRequest request,
                                      @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByUsername(principal.getUsername());
        User recipient = userService.findByUsername(request.recipientUsername());
        Conversation conv = chatService.findOrCreate(user, recipient);
        return new ConversationSummary(conv.getId(), recipient.getUsername(), recipient.getDisplayName(),
                recipient.getRole().name(), 0, null, null, false);
    }

    @GetMapping("/conversations")
    public List<ConversationSummary> list(@AuthenticationPrincipal UserDetails principal) {
        return chatService.summaries(principal.getUsername());
    }

    @GetMapping("/history")
    public MessagesPage history(@RequestParam String with,
                                @RequestParam(required = false) Long before,
                                @AuthenticationPrincipal UserDetails principal) {
        return chatService.history(principal.getUsername(), with, before);
    }

    @GetMapping("/unread-count")
    public java.util.Map<String, Long> unread(@AuthenticationPrincipal UserDetails principal) {
        return java.util.Map.of("unread", chatService.unreadTotal(principal.getUsername()));
    }

    /** Invio via REST (es. "condividi in chat" dal dettaglio di una connessione): stessa consegna del WebSocket. */
    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public com.atlante.dto.ChatDtos.MessageResponse sendMessage(
            @RequestBody com.atlante.dto.ChatDtos.SendMessage request,
            @AuthenticationPrincipal UserDetails principal) {
        return chatService.send(principal.getUsername(), request.recipient(), request.text(),
                request.tempId(), request.connectionId());
    }

    @PatchMapping("/conversations/{conversationId}/delivered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markDelivered(@PathVariable Long conversationId,
                              @AuthenticationPrincipal UserDetails principal) {
        chatService.markDelivered(conversationId, principal.getUsername());
    }

    @PostMapping("/suggest")
    public SuggestionResponse suggest(@Valid @RequestBody SuggestionRequest request,
                                      @AuthenticationPrincipal UserDetails principal) {
        return new SuggestionResponse(
                assistantService.suggest(request.conversationId(), principal.getUsername()));
    }
}
