package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "spring.ai.azure.openai")
record AzureEmbeddingProperties(String endpoint, @NestedConfigurationProperty Embedding embedding) {

    record Embedding(Options options) {
    }

    record Options(String deploymentName) {
    }
}
