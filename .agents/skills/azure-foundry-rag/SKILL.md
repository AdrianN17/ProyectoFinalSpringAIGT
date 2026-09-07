---
name: azure-foundry-rag
description: >-
  Build Retrieval-Augmented Generation (RAG) pipelines against Azure AI Foundry (Azure OpenAI
  chat + embedding models) using Spring AI in Java. Use when implementing document ingestion,
  chunking, embeddings, vector search, or grounded chat completion in this Spring Boot codebase.
---

# RAG with Azure AI Foundry (Spring AI, Java)

## Overview

This project already depends on `spring-ai-pdf-document-reader` and a vector store starter (Qdrant) via the `spring-ai-bom`. This skill covers wiring **Azure AI Foundry** (Azure OpenAI chat + embedding deployments) as the model provider for a RAG pipeline, optionally paired with Azure AI Search as the vector store instead of Qdrant.

> Spring AI's Azure module surface changes across versions. Before implementing, verify current property names and advisor classes for the pinned `springAiVersion` (see `build.gradle`) via the `microsoft-docs` skill or Spring AI reference docs.

## Installation

Add to `build.gradle` (the `spring-ai-bom` import already present pins versions):

```gradle
implementation 'org.springframework.ai:spring-ai-starter-model-azure-openai'
implementation 'org.springframework.ai:spring-ai-starter-vector-store-azure' // only if replacing/adding to Qdrant
```

## Environment Variables

| Variable | Required | Purpose |
|---|---|---|
| `AZURE_OPENAI_ENDPOINT` | Yes | Azure AI Foundry / Azure OpenAI resource endpoint |
| `AZURE_OPENAI_API_KEY` | Only for legacy key auth | Fallback when Entra ID auth isn't configured |
| `spring.ai.azure.openai.chat.options.deployment-name` | Yes | Chat model deployment name |
| `spring.ai.azure.openai.embedding.options.deployment-name` | Yes | Embedding model deployment name |

## Authentication

Prefer Microsoft Entra ID over API keys — avoid hardcoding credentials.

**Local dev / production (recommended):** expose a `TokenCredential` bean; Spring AI's Azure OpenAI auto-configuration picks up a user-defined `OpenAIClientBuilder`/`TokenCredential` bean over API-key config.

```java
@Bean
TokenCredential azureCredential() {
    // Local dev: works as-is with Azure CLI/VS Code login.
    // Production: constrain via AZURE_TOKEN_CREDENTIALS=prod, or return
    // new ManagedIdentityCredentialBuilder().build() directly.
    return new DefaultAzureCredentialBuilder().build();
}
```

### Legacy: API key (existing keyed deployments)

```yaml
spring:
  ai:
    azure:
      openai:
        api-key: ${AZURE_OPENAI_API_KEY}
        endpoint: ${AZURE_OPENAI_ENDPOINT}
```

New code should use the `TokenCredential` bean above; keep this only for existing keyed deployments.

## Core Workflow

### 1. Ingest documents into the vector store

```java
@Bean
CommandLineRunner ingest(VectorStore vectorStore, ResourceLoader loader) {
    return args -> {
        var reader = new PagePdfDocumentReader(loader.getResource("classpath:docs/policy.pdf"));
        var splitter = new TokenTextSplitter();
        vectorStore.add(splitter.apply(reader.get()));
    };
}
```

### 2. Retrieve + generate with `ChatClient`

```java
@RestController
class RagController {
    private final ChatClient chatClient;

    RagController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
            .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
            .build();
    }

    @GetMapping("/ask")
    String ask(@RequestParam String question) {
        return chatClient.prompt(question).call().content();
    }
}
```

`QuestionAnswerAdvisor` performs the retrieval + prompt augmentation in one step. Newer Spring AI versions may expose `RetrievalAugmentationAdvisor` with more control over the query-transformation/retrieval/generation stages — check the pinned version's docs before choosing between them.

## Vector Store Choice

| Store | When to use |
|---|---|
| Qdrant (already configured in this project) | Self-hosted/local vector search, no extra Azure dependency |
| Azure AI Search | Keep everything in Azure Foundry's ecosystem, built-in hybrid/semantic search, managed scaling |

Both implement Spring AI's `VectorStore` interface, so the RAG code above (`vectorStore.add(...)`, `QuestionAnswerAdvisor`) is unchanged when swapping stores — only the Gradle dependency and `application.yaml` config differ.

## Best Practices

1. Prefer Microsoft Entra ID (`DefaultAzureCredential`/`ManagedIdentityCredential`) over API keys; keep `AZURE_OPENAI_API_KEY` only as a legacy/local fallback.
2. Chunk documents with overlap sized for the embedding model's context window; keep source/page metadata on each chunk for citations.
3. Keep chat and embedding deployment names in configuration, never hardcoded in Java.
4. Verify current Spring AI Azure starter property names and advisor APIs against Spring AI docs/`microsoft-docs` before implementing — this API surface changes across releases.
5. Return retrieved-context citations alongside generated answers where the use case requires traceability.
6. Test retrieval independently (assert the right chunks come back) before testing end-to-end answer quality.
7. Don't log full document contents at INFO level if source documents may contain sensitive data.
8. Follow this repo's [hexagonal-architecture](../hexagonal-architecture/SKILL.md) skill: define an outbound port (e.g. `EmbeddingGateway`, `RagQueryPort`) in `application/port/out`, and put the Spring AI/Azure wiring behind an adapter in `infrastructure/adapter/out/ai` — don't call `ChatClient`/`VectorStore` directly from domain code.
