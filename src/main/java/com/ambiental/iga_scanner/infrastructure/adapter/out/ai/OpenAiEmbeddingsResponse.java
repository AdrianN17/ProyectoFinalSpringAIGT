package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Standard OpenAI-compatible embeddings response shape, as served by Azure AI Foundry's
// /openai/v1/embeddings endpoint (Foundry Models sold by Azure).
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiEmbeddingsResponse(List<Data> data, String model, Usage usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Data(float[] embedding, int index) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") Integer promptTokens, @JsonProperty("total_tokens") Integer totalTokens) {
    }
}
