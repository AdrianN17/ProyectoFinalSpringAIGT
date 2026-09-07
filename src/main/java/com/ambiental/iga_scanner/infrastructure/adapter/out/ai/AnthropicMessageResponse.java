package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Native Anthropic Messages API response shape, as served by Azure AI Foundry's
// /anthropic/v1/messages endpoint.
@JsonIgnoreProperties(ignoreUnknown = true)
record AnthropicMessageResponse(List<ContentBlock> content, Usage usage) {

    String text() {
        if (content == null) {
            return "";
        }
        return content.stream()
                .filter(block -> "text".equals(block.type()))
                .map(ContentBlock::text)
                .reduce("", String::concat);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentBlock(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("input_tokens") Integer inputTokens, @JsonProperty("output_tokens") Integer outputTokens) {
    }
}
