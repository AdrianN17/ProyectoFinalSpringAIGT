package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.ai.AnthropicChatGenerationAdapterMessageError.GENERATION_FAILED;

import com.ambiental.iga_scanner.application.port.out.ChatGenerationPort;
import com.ambiental.iga_scanner.domain.GeneratedAnswer;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Calls Claude on Azure AI Foundry's native Anthropic Messages API directly, authenticating
// with a Microsoft Entra ID token from the local `az login` session (no API key involved).
@Component
@ConditionalOnProperty(prefix = "iga.scanner.ai", name = "provider", havingValue = "anthropic", matchIfMissing = true)
class AnthropicChatGenerationAdapter implements ChatGenerationPort {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final AzureFoundryTokenProvider tokenProvider;
    private final AiModelProperties properties;

    AnthropicChatGenerationAdapter(AzureFoundryTokenProvider tokenProvider, AiModelProperties properties) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    @Override
    public GeneratedAnswer generate(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.deploymentName(),
                "max_tokens", properties.maxTokens(),
                "temperature", properties.temperature(),
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt)));

        try {
            AnthropicMessageResponse response = restClient.post()
                    .uri("/v1/messages")
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("Authorization", "Bearer " + tokenProvider.bearerToken())
                    .body(requestBody)
                    .retrieve()
                    .body(AnthropicMessageResponse.class);

            String answer = response == null ? "" : response.text();
            var usage = response == null ? null : response.usage();
            Integer inputTokens = usage == null ? null : usage.inputTokens();
            Integer outputTokens = usage == null ? null : usage.outputTokens();
            return new GeneratedAnswer(answer, inputTokens, outputTokens);
        } catch (RestClientException e) {
            throw new AiGenerationException(GENERATION_FAILED, e);
        }
    }
}
