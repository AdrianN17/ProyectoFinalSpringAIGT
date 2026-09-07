package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iga.scanner.ai")
record AiModelProperties(String provider, String baseUrl, String deploymentName, int maxTokens, double temperature) {
}
