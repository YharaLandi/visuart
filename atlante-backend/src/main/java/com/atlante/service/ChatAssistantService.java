package com.atlante.service;

import com.atlante.model.Conversation;
import com.atlante.model.Message;
import com.atlante.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Suggerisce all'utente il prossimo messaggio della chat. Il suggerimento non viene salvato. */
@Service
@RequiredArgsConstructor
public class ChatAssistantService {

    private static final String INSTRUCTIONS = """
            Sei un assistente per una chat tra appassionati di cinema e arte visiva, in italiano.
            Analizza la conversazione e suggerisci un breve messaggio (massimo 2 frasi)
            che l'utente potrebbe inviare per continuarla.
            Rispondi SOLO con il testo del messaggio suggerito, senza spiegazioni o prefissi.
            """;

    private final ChatService chatService;
    private final MessageRepository messageRepository;
    private final LlmService llmService;

    @Transactional(readOnly = true)
    public String suggest(Long conversationId, String username) {
        Conversation conv = chatService.requireParticipant(conversationId, username);

        List<Message> recent = messageRepository.latest(conv.getId(), Limit.of(10)).reversed();
        String history = recent.stream()
                .map(m -> m.getSender().getUsername() + ": " + m.getText())
                .collect(Collectors.joining("\n"));

        String prompt = "Conversazione recente:\n\n" + history
                + "\n\nSuggerisci il prossimo messaggio che \"" + username + "\" potrebbe inviare.";
        return llmService.generate(INSTRUCTIONS, prompt).trim();
    }
}
