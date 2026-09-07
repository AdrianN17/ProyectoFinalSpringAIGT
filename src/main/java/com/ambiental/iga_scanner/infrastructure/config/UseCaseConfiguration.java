package com.ambiental.iga_scanner.infrastructure.config;

import com.ambiental.iga_scanner.application.port.in.AskChatUseCase;
import com.ambiental.iga_scanner.application.port.in.GetSessionHistoryUseCase;
import com.ambiental.iga_scanner.application.port.in.UploadPdfUseCase;
import com.ambiental.iga_scanner.application.port.out.ChatGenerationPort;
import com.ambiental.iga_scanner.application.port.out.ChatRetrievalPort;
import com.ambiental.iga_scanner.application.port.out.DocumentCatalogPort;
import com.ambiental.iga_scanner.application.port.out.DocumentIngestionPort;
import com.ambiental.iga_scanner.application.port.out.HistoryRepositoryPort;
import com.ambiental.iga_scanner.application.service.ChatService;
import com.ambiental.iga_scanner.application.service.HistoryQueryService;
import com.ambiental.iga_scanner.application.service.PdfUploadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Wires the framework-agnostic application services as Spring beans here, in infrastructure,
// so application/ and domain/ stay free of any Spring (or other external) dependency.
@Configuration
class UseCaseConfiguration {

    @Bean
    AskChatUseCase askChatUseCase(ChatRetrievalPort chatRetrievalPort, ChatGenerationPort chatGenerationPort,
            HistoryRepositoryPort historyRepositoryPort, DocumentCatalogPort documentCatalogPort) {
        return new ChatService(chatRetrievalPort, chatGenerationPort, historyRepositoryPort, documentCatalogPort);
    }

    @Bean
    GetSessionHistoryUseCase getSessionHistoryUseCase(HistoryRepositoryPort historyRepositoryPort) {
        return new HistoryQueryService(historyRepositoryPort);
    }

    @Bean
    UploadPdfUseCase uploadPdfUseCase(DocumentIngestionPort documentIngestionPort) {
        return new PdfUploadService(documentIngestionPort);
    }
}
