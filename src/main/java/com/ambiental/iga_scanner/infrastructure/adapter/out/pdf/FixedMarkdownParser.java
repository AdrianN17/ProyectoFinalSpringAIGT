package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfIngestionAdapterMessageError.NO_SECTIONS_FOUND;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Parses the strict "## Secci\u00f3n: / Tags:" Markdown format produced by
 * {@link MarkdownSectionOrganizer} into one {@link Document} per section, tagging each with
 * {@code source}, {@code section} and {@code tags} metadata so the vector store can later be
 * filtered/segmented by them.
 */
@Component
class FixedMarkdownParser {

    private static final Pattern SECTION_HEADER = Pattern.compile("^##\\s*Secci\\u00f3n:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern TAGS_LINE = Pattern.compile("^Tags:\\s*(.+)$");

    static final String SOURCE_METADATA_KEY = "source";
    static final String SECTION_METADATA_KEY = "section";
    static final String TAGS_METADATA_KEY = "tags";

    List<Document> parse(String fixedMarkdown, String fileName) {
        Matcher matcher = SECTION_HEADER.matcher(fixedMarkdown);
        List<int[]> headerRanges = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            headerRanges.add(new int[] {matcher.start(), matcher.end()});
            titles.add(matcher.group(1).trim());
        }
        if (headerRanges.isEmpty()) {
            throw new PdfProcessingException(NO_SECTIONS_FOUND);
        }

        List<Document> documents = new ArrayList<>(headerRanges.size());
        for (int i = 0; i < headerRanges.size(); i++) {
            int bodyStart = headerRanges.get(i)[1];
            int bodyEnd = i + 1 < headerRanges.size() ? headerRanges.get(i + 1)[0] : fixedMarkdown.length();
            String body = fixedMarkdown.substring(bodyStart, bodyEnd).strip();

            String tags = "";
            String[] lines = body.split("\\R", 2);
            Matcher tagsMatcher = lines.length > 0 ? TAGS_LINE.matcher(lines[0].trim()) : null;
            String content = body;
            if (tagsMatcher != null && tagsMatcher.matches()) {
                tags = tagsMatcher.group(1).trim();
                content = lines.length > 1 ? lines[1].strip() : "";
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put(SOURCE_METADATA_KEY, fileName);
            metadata.put(SECTION_METADATA_KEY, titles.get(i));
            metadata.put(TAGS_METADATA_KEY, tags);
            documents.add(new Document(content, metadata));
        }
        return documents;
    }
}
