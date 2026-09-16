package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iga.scanner.rag")
record RagRetrievalProperties(double similarityThreshold, double maxScoreGap) {
}
