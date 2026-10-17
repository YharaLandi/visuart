package com.atlante.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmResponse(
        String id,
        String model,
        List<Choice> choices,
        Usage usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(int index, Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content, String refusal) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(@JsonProperty("prompt_tokens") int promptTokens,
                        @JsonProperty("completion_tokens") int completionTokens,
                        @JsonProperty("total_tokens") int totalTokens) {
    }

    public String firstText() {
        if (choices == null || choices.isEmpty()) return "";
        Message msg = choices.getFirst().message();
        if (msg == null || msg.content() == null) return "";
        return msg.content();
    }

    public String finishReason() {
        if (choices == null || choices.isEmpty()) return null;
        return choices.getFirst().finishReason();
    }

    public int inputTokens() {
        return usage == null ? 0 : usage.promptTokens();
    }

    public int outputTokens() {
        return usage == null ? 0 : usage.completionTokens();
    }
}
