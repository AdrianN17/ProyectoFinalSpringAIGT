package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.ai.AzureOpenAiChatGenerationAdapterMessageError.GENERATION_FAILED;

import com.ambiental.iga_scanner.application.port.out.ChatGenerationPort;
import com.ambiental.iga_scanner.domain.GeneratedAnswer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// Calls Azure AI Foundry's OpenAI-compatible chat completions endpoint (gpt-5-mini), a
// reasoning model that requires `max_completion_tokens` instead of `max_tokens` and does not
// support `temperature`/`top_p`/penalty parameters. Authenticates with a Microsoft Entra ID
// token from `az login` (see AzureFoundryTokenProvider). This is the only ChatGenerationPort
// implementation; ChatService and the rest of the application never know the concrete model.
@Component
class AzureOpenAiChatGenerationAdapter implements ChatGenerationPort {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiChatGenerationAdapter.class);

    private final RestClient restClient;
    private final AzureFoundryTokenProvider tokenProvider;
    private final AiModelProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    AzureOpenAiChatGenerationAdapter(AzureFoundryTokenProvider tokenProvider, AiModelProperties properties) {
        // Buffer the request body so Content-Length can be computed up front; Azure's
        // Foundry passthrough rejects chunked-transfer requests (Spring's SimpleClientHttp-
        // RequestFactory always streams unknown-length bodies as chunked, so the JSON body
        // must be pre-serialized to a byte[] - see toJsonBytes()).
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    @Override
    public GeneratedAnswer generate(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.deploymentName(),
                "max_completion_tokens", properties.maxCompletionTokens(),
                "reasoning_effort", properties.reasoningEffort(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        log.info("Azure OpenAI chat completions request: model={}, max_completion_tokens={}, "
                        + "reasoning_effort={}, system_prompt_chars={}, user_prompt_chars={}",
                properties.deploymentName(), properties.maxCompletionTokens(), properties.reasoningEffort(),
                systemPrompt == null ? 0 : systemPrompt.length(), userPrompt == null ? 0 : userPrompt.length());

        try {
            byte[] jsonBody = toJsonBytes(requestBody);
            log.info("Azure OpenAI chat completions request body size: {} bytes", jsonBody.length);
            OpenAiChatCompletionResponse response = RateLimitRetryTemplate.execute("Azure OpenAI chat completions call",
                    () -> restClient.post()
                            .uri("/openai/v1/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + tokenProvider.bearerToken())
                            .body(jsonBody)
                            .retrieve()
                            .body(OpenAiChatCompletionResponse.class));

            String answer = response == null ? "" : response.text();
            var usage = response == null ? null : response.usage();
            Integer inputTokens = usage == null ? null : usage.promptTokens();
            Integer outputTokens = usage == null ? null : usage.completionTokens();
            Integer reasoningTokens = usage == null || usage.completionTokensDetails() == null
                    ? null : usage.completionTokensDetails().reasoningTokens();
            String finishReason = response == null ? null : response.finishReason();
            log.info("Azure OpenAI chat completions response: finish_reason={}, prompt_tokens={}, "
                            + "completion_tokens={}, reasoning_tokens={}, answer_chars={}",
                    finishReason, inputTokens, outputTokens, reasoningTokens, answer.length());
            if (answer.isBlank()) {
                log.warn("Azure OpenAI chat completions returned a blank answer. This commonly happens when "
                        + "max_completion_tokens ({}) is fully consumed by internal reasoning_tokens ({}) before "
                        + "any visible content is produced, or when finish_reason is 'length'/'content_filter' "
                        + "(finish_reason={}). Consider raising max-completion-tokens.",
                        properties.maxCompletionTokens(), reasoningTokens, finishReason);
            }
            return new GeneratedAnswer(answer, inputTokens, outputTokens);
        } catch (RestClientException e) {
            if (e instanceof RestClientResponseException responseException) {
                log.error("Azure OpenAI chat completions call failed with status {} and body: {}",
                        responseException.getStatusCode(), responseException.getResponseBodyAsString(), e);
            } else {
                log.error("Azure OpenAI chat completions call failed", e);
            }
            throw new AiGenerationException(GENERATION_FAILED, e);
        } catch (JsonProcessingException e) {
            throw new AiGenerationException(GENERATION_FAILED, e);
        }
    }

    // Serializing the body ourselves (instead of letting RestClient/Jackson stream it lazily)
    // lets ByteArrayHttpMessageConverter report an exact Content-Length up front, which Azure's
    // Foundry passthrough requires and otherwise rejects as chunked transfer encoding.
    private byte[] toJsonBytes(Map<String, Object> requestBody) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(requestBody);
    }
}
