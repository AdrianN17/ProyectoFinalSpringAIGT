package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.BASE_MARKDOWN_STAGING_ERROR;
import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.CLASSIFICATION_STAGING_ERROR;
import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.EXTRACTED_DIR_ERROR;
import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.ORIGINAL_STAGING_ERROR;
import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.SECTION_STAGING_ERROR;

import com.ambiental.iga_scanner.application.port.out.DocumentCatalogPort;
import com.ambiental.iga_scanner.application.port.out.DocumentIngestionPort;
import com.ambiental.iga_scanner.domain.DocumentStatus;
import com.ambiental.iga_scanner.domain.IngestedDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

// Pipeline: stage original PDF -> extract text per page (no images) -> convert to verbatim
// base Markdown -> (if enabled) classify with the chat LLM into logical sections, respecting
// fidelity to the source -> stage one Markdown file per section under /extracted, tagged with
// metadata that references the original PDF -> upload each section to Qdrant one at a time.
// If reorganization is disabled, falls back to the previous page-based chunking.
@Component
class PdfIngestionAdapter implements DocumentIngestionPort {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionAdapter.class);

    static final String SOURCE_METADATA_KEY = "source";
    static final String PAGE_METADATA_KEY = "page";
    static final String DOCUMENT_ID_METADATA_KEY = "document_id";
    static final String SECTION_INDEX_METADATA_KEY = "section_index";
    static final String SECTION_FILE_METADATA_KEY = "section_file";

    private final VectorStore vectorStore;
    private final DocumentCatalogPort documentCatalogPort;
    private final TokenTextSplitter textSplitter;
    private final Path processDir;
    private final boolean reorganizeEnabled;
    private final MarkdownSectionOrganizer markdownSectionOrganizer;
    private final FixedMarkdownParser fixedMarkdownParser;
    private final MarkdownFidelityGuard markdownFidelityGuard;

    PdfIngestionAdapter(VectorStore vectorStore, DocumentCatalogPort documentCatalogPort,
            PdfProcessingProperties properties, MarkdownSectionOrganizer markdownSectionOrganizer,
            FixedMarkdownParser fixedMarkdownParser, MarkdownFidelityGuard markdownFidelityGuard) {
        this.vectorStore = vectorStore;
        this.documentCatalogPort = documentCatalogPort;
        this.textSplitter = TokenTextSplitter.builder().build();
        this.processDir = Path.of(properties.processDir());
        this.reorganizeEnabled = properties.reorganizeEnabled();
        this.markdownSectionOrganizer = markdownSectionOrganizer;
        this.fixedMarkdownParser = fixedMarkdownParser;
        this.markdownFidelityGuard = markdownFidelityGuard;
    }

    @Override
    public IngestedDocument ingest(String fileName, byte[] pdfBytes) {
        UUID documentId = UUID.randomUUID();
        Path documentDir = processDir.resolve(documentId.toString());
        stageOriginal(documentDir, pdfBytes);

        var reader = new PagePdfDocumentReader(new ByteArrayResource(pdfBytes));
        List<Document> pages = reader.get();

        StringBuilder baseMarkdown = new StringBuilder();
        List<Document> taggedPages = new ArrayList<>(pages.size());
        for (Document page : pages) {
            int pageNumber = pageNumberOf(page);
            String markdown = PdfMarkdownConverter.toMarkdownPage(fileName, pageNumber, page.getText());
            baseMarkdown.append(markdown).append('\n');

            Map<String, Object> metadata = new HashMap<>(page.getMetadata());
            metadata.put(SOURCE_METADATA_KEY, fileName);
            metadata.put(PAGE_METADATA_KEY, pageNumber);
            metadata.put(DOCUMENT_ID_METADATA_KEY, documentId.toString());
            taggedPages.add(new Document(markdown, metadata));
        }
        stageBaseMarkdown(documentDir, baseMarkdown.toString());

        int chunkCount = reorganizeEnabled
                ? classifyAndIndexBySection(documentDir, fileName, documentId, baseMarkdown.toString())
                : indexByPage(taggedPages);

        var ingestedDocument = new IngestedDocument(documentId, fileName, chunkCount, DocumentStatus.PROCESSED);
        documentCatalogPort.recordUpload(ingestedDocument);
        return ingestedDocument;
    }

    // Fallback path (iga.scanner.pdf.reorganize-enabled=false): batch all page chunks into a
    // single vectorStore.add call, as before the AI classification step existed.
    private int indexByPage(List<Document> taggedPages) {
        List<Document> chunks = textSplitter.apply(taggedPages);
        vectorStore.add(chunks);
        return chunks.size();
    }

    // Sends the verbatim base markdown through the chat LLM (gpt-5-mini) to be
    // classified into logical sections, enforces the fidelity guardrail, then persists and
    // indexes each section individually.
    private int classifyAndIndexBySection(Path documentDir, String fileName, UUID documentId, String baseMarkdown) {
        String classificationRaw = markdownSectionOrganizer.organize(fileName, baseMarkdown);
        stageClassificationRaw(documentDir, classificationRaw);

        List<Document> sections = fixedMarkdownParser.parse(classificationRaw, fileName);
        markdownFidelityGuard.assertFaithful(baseMarkdown, sections);

        Path extractedDir = documentDir.resolve("extracted");
        createExtractedDir(extractedDir);

        int chunkCount = 0;
        int sectionIndex = 0;
        for (Document section : sections) {
            sectionIndex++;
            String sectionTitle = String.valueOf(section.getMetadata().get(FixedMarkdownParser.SECTION_METADATA_KEY));
            String sectionFileName = sectionFileName(sectionIndex, sectionTitle);

            Map<String, Object> metadata = new HashMap<>(section.getMetadata());
            metadata.put(DOCUMENT_ID_METADATA_KEY, documentId.toString());
            metadata.put(SECTION_INDEX_METADATA_KEY, sectionIndex);
            metadata.put(SECTION_FILE_METADATA_KEY, "extracted/" + sectionFileName);
            Document enrichedSection = new Document(section.getText(), metadata);

            stageSectionMarkdown(extractedDir, sectionFileName, enrichedSection, fileName, documentId);

            // Upload one section (or its sub-chunks, if it exceeds the model's context) at a
            // time, rather than batching every section of the document into one call.
            List<Document> chunks = textSplitter.apply(List.of(enrichedSection));
            for (Document chunk : chunks) {
                vectorStore.add(List.of(chunk));
                chunkCount++;
            }
            log.info("Indexed section {}/{} ('{}') from '{}' as {} chunk(s)", sectionIndex, sections.size(),
                    sectionTitle, fileName, chunks.size());
        }
        return chunkCount;
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

    private void stageBaseMarkdown(Path documentDir, String markdown) {
        try {
            Files.writeString(documentDir.resolve("base.md"), markdown);
        } catch (IOException e) {
            throw new PdfProcessingException(BASE_MARKDOWN_STAGING_ERROR, e);
        }
    }

    private void stageClassificationRaw(Path documentDir, String classificationRaw) {
        try {
            Files.writeString(documentDir.resolve("classification-raw.md"), classificationRaw);
        } catch (IOException e) {
            throw new PdfProcessingException(CLASSIFICATION_STAGING_ERROR, e);
        }
    }

    private void createExtractedDir(Path extractedDir) {
        try {
            Files.createDirectories(extractedDir);
        } catch (IOException e) {
            throw new PdfProcessingException(EXTRACTED_DIR_ERROR, e);
        }
    }

    private void stageSectionMarkdown(Path extractedDir, String sectionFileName, Document section, String fileName,
            UUID documentId) {
        try {
            Files.writeString(extractedDir.resolve(sectionFileName), sectionFileContent(section, fileName, documentId));
        } catch (IOException e) {
            throw new PdfProcessingException(SECTION_STAGING_ERROR, e);
        }
    }

    // Human-readable frontmatter on top of the verbatim section content, mirroring the same
    // metadata (source PDF, document id, section, tags) stored alongside the chunk in Qdrant.
    private String sectionFileContent(Document section, String fileName, UUID documentId) {
        Map<String, Object> metadata = section.getMetadata();
        StringBuilder content = new StringBuilder();
        content.append("---\n");
        content.append("source: ").append(fileName).append('\n');
        content.append("document_id: ").append(documentId).append('\n');
        content.append("section: ").append(metadata.getOrDefault(FixedMarkdownParser.SECTION_METADATA_KEY, "")).append('\n');
        content.append("tags: ").append(metadata.getOrDefault(FixedMarkdownParser.TAGS_METADATA_KEY, "")).append('\n');
        content.append("---\n\n");
        content.append(section.getText());
        return content.toString();
    }

    private String sectionFileName(int sectionIndex, String sectionTitle) {
        String normalized = Normalizer.normalize(sectionTitle == null ? "" : sectionTitle, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (slug.isBlank()) {
            slug = "seccion";
        }
        return String.format("%02d-%s.md", sectionIndex, slug);
    }
}
