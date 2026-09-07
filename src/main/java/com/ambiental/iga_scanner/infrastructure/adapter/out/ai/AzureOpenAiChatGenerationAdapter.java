package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.ai.AzureOpenAiChatGenerationAdapterMessageError.GENERATION_FAILED;

import com.ambiental.iga_scanner.application.port.out.ChatGenerationPort;
import com.ambiental.iga_scanner.domain.GeneratedAnswer;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Calls an Azure OpenAI (or other OpenAI-compatible) model on Azure AI Foundry's v1 chat
// completions endpoint, authenticating with a Microsoft Entra ID token from `az login`.
// Swap to this provider by setting iga.scanner.ai.provider=azure-openai; ChatService and the
// rest of the application never know which adapter is active.
@Component
@ConditionalOnProperty(prefix = "iga.scanner.ai", name = "provider", havingValue = "azure-openai")
class AzureOpenAiChatGenerationAdapter implements ChatGenerationPort {

    private final RestClient restClient;
    private final AzureFoundryTokenProvider tokenProvider;
    private final AiModelProperties properties;

    AzureOpenAiChatGenerationAdapter(AzureFoundryTokenProvider tokenProvider, AiModelProperties properties) {
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
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        try {
            OpenAiChatCompletionResponse response = restClient.post()
                    .uri("/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + tokenProvider.bearerToken())
                    .body(requestBody)
                    .retrieve()
                    .body(OpenAiChatCompletionResponse.class);

            String answer = response == null ? "" : response.text();
            var usage = response == null ? null : response.usage();
            Integer inputTokens = usage == null ? null : usage.promptTokens();
            Integer outputTokens = usage == null ? null : usage.completionTokens();
            return new GeneratedAnswer(answer, inputTokens, outputTokens);
        } catch (RestClientException e) {
            throw new AiGenerationException(GENERATION_FAILED, e);
        }
    }
}
