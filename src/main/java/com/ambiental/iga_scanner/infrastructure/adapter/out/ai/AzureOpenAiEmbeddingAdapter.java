package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.ai.AzureOpenAiEmbeddingAdapterMessageError.EMBEDDING_FAILED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// Calls Azure AI Foundry's OpenAI-compatible embeddings endpoint, authenticating with a
// Microsoft Entra ID token from `az login` (see AzureFoundryTokenProvider). Registered as
// the EmbeddingModel bean that QdrantVectorStoreAutoConfiguration wires into the VectorStore
// used by QdrantChatRetrievalAdapter and the PDF ingestion pipeline.
@Component
class AzureOpenAiEmbeddingAdapter implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiEmbeddingAdapter.class);
    private static final int MAX_ATTEMPTS = 5;

    private final RestClient restClient;
    private final AzureFoundryTokenProvider tokenProvider;
    private final String deploymentName;
    private final EmbeddingRequestPacer pacer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    AzureOpenAiEmbeddingAdapter(AzureFoundryTokenProvider tokenProvider, AzureEmbeddingProperties properties,
            EmbeddingRequestPacer pacer) {
        // Buffer the request body so Content-Length can be computed up front; Azure's
        // Foundry passthrough rejects chunked-transfer requests (Spring's SimpleClientHttp-
        // RequestFactory always streams unknown-length bodies as chunked, so the JSON body
        // must be pre-serialized to a byte[] - see toJsonBytes()).
        this.restClient = RestClient.builder()
                .baseUrl(properties.endpoint())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        this.tokenProvider = tokenProvider;
        this.deploymentName = properties.embedding().options().deploymentName();
        this.pacer = pacer;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        Map<String, Object> requestBody = Map.of(
                "model", deploymentName,
                "input", request.getInstructions());

        try {
            byte[] jsonBody = toJsonBytes(requestBody);
            OpenAiEmbeddingsResponse response = callWithPacing(jsonBody);

            List<Embedding> embeddings = response == null
                    ? List.of()
                    : response.data().stream()
                            .map(data -> new Embedding(data.embedding(), data.index()))
                            .toList();
            String model = response == null ? deploymentName : response.model();
            var usage = logAndBuildUsage(response, request.getInstructions().size());
            return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata(model, usage));
        } catch (RestClientException e) {
            if (e instanceof RestClientResponseException responseException) {
                log.error("Azure OpenAI embeddings call failed with status {} and body: {}",
                        responseException.getStatusCode(), responseException.getResponseBodyAsString(), e);
            } else {
                log.error("Azure OpenAI embeddings call failed", e);
            }
            throw new AiGenerationException(EMBEDDING_FAILED, e);
        } catch (JsonProcessingException e) {
            throw new AiGenerationException(EMBEDDING_FAILED, e);
        }
    }

    @Override
    public float[] embed(Document document) {
        return embed(getEmbeddingContent(document));
    }

    private DefaultUsage logAndBuildUsage(OpenAiEmbeddingsResponse response, int inputCount) {
        Integer promptTokens = response == null || response.usage() == null ? null : response.usage().promptTokens();
        Integer totalTokens = response == null || response.usage() == null ? null : response.usage().totalTokens();
        log.info("Azure OpenAI embeddings usage: inputs={}, prompt_tokens={}, total_tokens={}",
                inputCount, promptTokens, totalTokens);
        return new DefaultUsage(promptTokens, 0, totalTokens, response == null ? null : response.usage());
    }

    // Every actual network attempt — the first try or any retry after a 429 — must wait
    // its turn on the shared pacer, which enforces a hard floor between calls regardless of
    // how many texts are batched into this particular request.
    private OpenAiEmbeddingsResponse callWithPacing(byte[] jsonBody) {
        int attempt = 0;
        while (true) {
            attempt++;
            pacer.awaitTurn();
            try {
                return restClient.post()
                        .uri("/openai/v1/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenProvider.bearerToken())
                        .body(jsonBody)
                        .retrieve()
                        .body(OpenAiEmbeddingsResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt >= MAX_ATTEMPTS) {
                    log.error("Azure OpenAI embeddings call failed after {} attempts due to repeated rate limiting",
                            attempt, e);
                    throw e;
                }
                log.warn("Azure OpenAI embeddings call was rate limited (attempt {}/{}); "
                        + "next attempt will wait for its turn on the pacer", attempt, MAX_ATTEMPTS);
            }
        }
    }

    // Serializing the body ourselves (instead of letting RestClient/Jackson stream it lazily)
    // lets ByteArrayHttpMessageConverter report an exact Content-Length up front, which Azure's
    // Foundry passthrough requires and otherwise rejects as chunked transfer encoding.
    private byte[] toJsonBytes(Map<String, Object> requestBody) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(requestBody);
    }
}
