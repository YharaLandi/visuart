package com.atlante.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** Corpo di una richiesta /chat/completions (formato compatibile OpenAI, usato anche da Gemini). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LlmRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        List<Turn> messages) {

    public record Turn(String role, String content) {

        public static Turn system(String content) {
            return new Turn("system", content);
        }

        public static Turn user(String content) {
            return new Turn("user", content);
        }
    }

    public static LlmRequest of(String model, int maxTokens, String instructions, String userText) {
        List<Turn> turns = new ArrayList<>();
        if (instructions != null && !instructions.isBlank()) {
            turns.add(Turn.system(instructions));
        }
        turns.add(Turn.user(userText));
        return new LlmRequest(model, maxTokens, turns);
    }
}
