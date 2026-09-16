package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iga.scanner.pdf")
public record PdfProcessingProperties(String processDir, boolean reorganizeEnabled, double fidelityMinRatio) {
}
