package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Converts raw extracted PDF page text into Markdown without summarizing or paraphrasing:
 * headings mark page boundaries, and lines that look column-aligned (candidate tables) are
 * wrapped in a fenced code block so their original shape/spacing is preserved verbatim.
 */
final class PdfMarkdownConverter {

    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("\\S+ {2,}\\S+");

    private PdfMarkdownConverter() {
    }

    static String toMarkdownPage(String fileName, int pageNumber, String rawPageText) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("## ").append(fileName).append(" \u2014 p\u00e1gina ").append(pageNumber).append("\n\n");
        markdown.append(preserveTables(rawPageText == null ? "" : rawPageText));
        markdown.append('\n');
        return markdown.toString();
    }

    private static String preserveTables(String rawText) {
        String[] lines = rawText.split("\\R", -1);
        StringBuilder result = new StringBuilder();
        List<String> tableBuffer = new ArrayList<>();

        for (String line : lines) {
            if (looksLikeTableRow(line)) {
                tableBuffer.add(line);
                continue;
            }
            flushTableBuffer(result, tableBuffer);
            result.append(line).append('\n');
        }
        flushTableBuffer(result, tableBuffer);
        return result.toString();
    }

    private static boolean looksLikeTableRow(String line) {
        return TABLE_ROW_PATTERN.matcher(line).find();
    }

    private static void flushTableBuffer(StringBuilder result, List<String> tableBuffer) {
        if (tableBuffer.isEmpty()) {
            return;
        }
        result.append("```\n");
        tableBuffer.forEach(row -> result.append(row).append('\n'));
        result.append("```\n");
        tableBuffer.clear();
    }
}
