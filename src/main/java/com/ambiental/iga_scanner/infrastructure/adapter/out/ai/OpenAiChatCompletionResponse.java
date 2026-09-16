package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Standard OpenAI-compatible chat completions response shape, as served by Azure AI
// Foundry's /openai/v1/chat/completions endpoint (Foundry Models sold by Azure).
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiChatCompletionResponse(List<Choice> choices, Usage usage) {

    String text() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        return choices.get(0).message().content();
    }

    String finishReason() {
        return choices == null || choices.isEmpty() ? null : choices.get(0).finishReason();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") Integer promptTokens, @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("completion_tokens_details") CompletionTokensDetails completionTokensDetails) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CompletionTokensDetails(@JsonProperty("reasoning_tokens") Integer reasoningTokens) {
    }
}
