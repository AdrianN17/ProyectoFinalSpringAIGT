package com.ambiental.iga_scanner.application.service;

import static com.ambiental.iga_scanner.application.service.GroundednessGuardMessageError.NOT_FOUND_ANSWER;

import com.ambiental.iga_scanner.domain.RetrievedChunk;
import java.util.List;

/**
 * Fail-closed guardrail: the model may only answer from Qdrant-retrieved chunks or the
 * ingested-document catalog, never from its own general/world knowledge. Enforced twice:
 * before generation (skip the model call entirely when there is nothing to ground on) and
 * after generation, by running the {@link AnswerGuardrail} strategy chain below — add a new
 * rule (e.g. PII redaction, length limits) by appending it to GUARDRAILS, nothing else changes.
 */
final class GroundednessGuard {

    private static final List<AnswerGuardrail> GUARDRAILS = List.of(
            GroundednessGuard::rejectBlankOrAlreadyDeclined,
            GroundednessGuard::requireKnownSourceCitation);

    private GroundednessGuard() {
    }

    static boolean hasContext(List<RetrievedChunk> chunks, List<String> catalog) {
        return (chunks != null && !chunks.isEmpty()) || (catalog != null && !catalog.isEmpty());
    }

    static String enforce(String generatedAnswer, List<RetrievedChunk> chunks, List<String> catalog) {
        String answer = generatedAnswer;
        for (AnswerGuardrail guardrail : GUARDRAILS) {
            answer = guardrail.apply(answer, chunks, catalog);
        }
        return answer;
    }

    private static String rejectBlankOrAlreadyDeclined(String answer, List<RetrievedChunk> chunks, List<String> catalog) {
        return (answer == null || answer.isBlank() || answer.contains(NOT_FOUND_ANSWER)) ? NOT_FOUND_ANSWER : answer;
    }

    private static String requireKnownSourceCitation(String answer, List<RetrievedChunk> chunks, List<String> catalog) {
        if (NOT_FOUND_ANSWER.equals(answer)) {
            return answer;
        }
        boolean citesChunkSource = chunks.stream()
                .anyMatch(chunk -> answer.contains("[Fuente: " + chunk.sourceDocument()));
        boolean citesCatalogEntry = catalog.stream()
                .anyMatch(name -> answer.contains("[Fuente: " + name));
        return (citesChunkSource || citesCatalogEntry) ? answer : NOT_FOUND_ANSWER;
    }
}
