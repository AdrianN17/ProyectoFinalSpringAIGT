package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.FIDELITY_CHECK_FAILED;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Business-rule guardrail: the AI-reorganized Markdown must be faithful to the original,
 * verbatim source (no summarizing, rewriting, or filling in missing data). Prompt
 * instructions alone cannot guarantee this, so this class adds a technical check: it
 * compares the amount of actual content (letters/digits, ignoring whitespace and Markdown
 * formatting noise) between the original page-by-page Markdown and the reorganized
 * sections. If the reorganized content is significantly smaller, the AI likely summarized
 * or dropped information, so the whole upload is aborted rather than indexing incomplete
 * data.
 *
 * <p>This threshold ({@code iga.scanner.pdf.fidelity-min-ratio}) is intentionally separate
 * from the chat retrieval similarity threshold ({@code iga.scanner.rag.similarity-threshold},
 * used by the QdrantChatRetrievalAdapter in the sibling {@code out.ai} package): they measure
 * completely different things (batch-time content-fidelity ratio vs. query-time vector
 * similarity), so tuning one must never require touching or redeploying the other.
 */
@Component
class MarkdownFidelityGuard {

    private static final Logger log = LoggerFactory.getLogger(MarkdownFidelityGuard.class);

    private final double minContentRatio;

    MarkdownFidelityGuard(PdfProcessingProperties properties) {
        this.minContentRatio = properties.fidelityMinRatio();
    }

    void assertFaithful(String originalMarkdown, List<Document> sections) {
        long originalContentSize = contentSize(originalMarkdown);
        long reorganizedContentSize = sections.stream()
                .mapToLong(section -> contentSize(section.getText()))
                .sum();

        if (originalContentSize == 0) {
            return;
        }

        double ratio = (double) reorganizedContentSize / originalContentSize;
        log.info("Markdown fidelity check: original={} chars, reorganized={} chars, ratio={}, min_ratio={}",
                originalContentSize, reorganizedContentSize, String.format("%.2f", ratio), minContentRatio);

        if (ratio < minContentRatio) {
            throw new PdfProcessingException(FIDELITY_CHECK_FAILED);
        }
    }

    // Counts only letters and digits, ignoring whitespace/punctuation/Markdown syntax, so
    // that formatting differences (line breaks, heading markers, spacing) don't skew the
    // comparison between the original and reorganized content.
    private long contentSize(String text) {
        if (text == null) {
            return 0;
        }
        return text.chars().filter(Character::isLetterOrDigit).count();
    }
}
