package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.ai.AzureOpenAiEmbeddingAdapterMessageError.EMBEDDING_FAILED;

import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Calls Azure AI Foundry's OpenAI-compatible embeddings endpoint, authenticating with a
// Microsoft Entra ID token from `az login` (see AzureFoundryTokenProvider). Registered as
// the EmbeddingModel bean that QdrantVectorStoreAutoConfiguration wires into the VectorStore
// used by QdrantChatRetrievalAdapter and the PDF ingestion pipeline.
@Component
class AzureOpenAiEmbeddingAdapter implements EmbeddingModel {

    private final RestClient restClient;
    private final AzureFoundryTokenProvider tokenProvider;
    private final String deploymentName;

    AzureOpenAiEmbeddingAdapter(AzureFoundryTokenProvider tokenProvider, AzureEmbeddingProperties properties) {
        this.restClient = RestClient.builder().baseUrl(properties.endpoint()).build();
        this.tokenProvider = tokenProvider;
        this.deploymentName = properties.embedding().options().deploymentName();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        Map<String, Object> requestBody = Map.of(
                "model", deploymentName,
                "input", request.getInstructions());

        try {
            OpenAiEmbeddingsResponse response = restClient.post()
                    .uri("/openai/v1/embeddings")
                    .header("Authorization", "Bearer " + tokenProvider.bearerToken())
                    .body(requestBody)
                    .retrieve()
                    .body(OpenAiEmbeddingsResponse.class);

            List<Embedding> embeddings = response == null
                    ? List.of()
                    : response.data().stream()
                            .map(data -> new Embedding(data.embedding(), data.index()))
                            .toList();
            String model = response == null ? deploymentName : response.model();
            return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata(model, null));
        } catch (RestClientException e) {
            throw new AiGenerationException(EMBEDDING_FAILED, e);
        }
    }

    @Override
    public float[] embed(Document document) {
        return embed(getEmbeddingContent(document));
    }
}
