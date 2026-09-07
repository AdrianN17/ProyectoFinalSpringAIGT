package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.MARKDOWN_STAGING_ERROR;
import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.ORIGINAL_STAGING_ERROR;

import com.ambiental.iga_scanner.application.port.out.DocumentCatalogPort;
import com.ambiental.iga_scanner.application.port.out.DocumentIngestionPort;
import com.ambiental.iga_scanner.domain.DocumentStatus;
import com.ambiental.iga_scanner.domain.IngestedDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

// Pipeline: stage original PDF -> extract text per page (no images) -> convert to Markdown
// (verbatim, tables kept as-is) -> stage Markdown -> chunk -> tag with the source file name -> index in Qdrant.
@Component
class PdfIngestionAdapter implements DocumentIngestionPort {

    static final String SOURCE_METADATA_KEY = "source";
    static final String PAGE_METADATA_KEY = "page";

    private final VectorStore vectorStore;
    private final DocumentCatalogPort documentCatalogPort;
    private final TokenTextSplitter textSplitter;
    private final Path processDir;

    PdfIngestionAdapter(VectorStore vectorStore, DocumentCatalogPort documentCatalogPort,
            PdfProcessingProperties properties) {
        this.vectorStore = vectorStore;
        this.documentCatalogPort = documentCatalogPort;
        this.textSplitter = new TokenTextSplitter();
        this.processDir = Path.of(properties.processDir());
    }

    @Override
    public IngestedDocument ingest(String fileName, byte[] pdfBytes) {
        UUID documentId = UUID.randomUUID();
        Path documentDir = processDir.resolve(documentId.toString());
        stageOriginal(documentDir, pdfBytes);

        var reader = new PagePdfDocumentReader(new ByteArrayResource(pdfBytes));
        List<Document> pages = reader.get();

        StringBuilder fullMarkdown = new StringBuilder();
        List<Document> taggedPages = new ArrayList<>(pages.size());
        for (Document page : pages) {
            int pageNumber = pageNumberOf(page);
            String markdown = PdfMarkdownConverter.toMarkdownPage(fileName, pageNumber, page.getText());
            fullMarkdown.append(markdown).append('\n');

            Map<String, Object> metadata = new HashMap<>(page.getMetadata());
            metadata.put(SOURCE_METADATA_KEY, fileName);
            metadata.put(PAGE_METADATA_KEY, pageNumber);
            taggedPages.add(new Document(markdown, metadata));
        }
        stageMarkdown(documentDir, fullMarkdown.toString());

        List<Document> chunks = textSplitter.apply(taggedPages);
        vectorStore.add(chunks);

        var ingestedDocument = new IngestedDocument(documentId, fileName, chunks.size(), DocumentStatus.PROCESSED);
        documentCatalogPort.recordUpload(ingestedDocument);
        return ingestedDocument;
    }

    private int pageNumberOf(Document page) {
        // Spring AI's PagePdfDocumentReader stores a 0-indexed "page_number" metadata entry.
        Object pageNumber = page.getMetadata().get("page_number");
        return pageNumber instanceof Number number ? number.intValue() + 1 : 0;
    }

    private void stageOriginal(Path documentDir, byte[] pdfBytes) {
        try {
            Files.createDirectories(documentDir);
            Files.write(documentDir.resolve("original.pdf"), pdfBytes);
        } catch (IOException e) {
            throw new PdfProcessingException(ORIGINAL_STAGING_ERROR, e);
        }
    }

    private void stageMarkdown(Path documentDir, String markdown) {
        try {
            Files.writeString(documentDir.resolve("converted.md"), markdown);
        } catch (IOException e) {
            throw new PdfProcessingException(MARKDOWN_STAGING_ERROR, e);
        }
    }
}
