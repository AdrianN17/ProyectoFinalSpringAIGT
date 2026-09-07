package com.ambiental.iga_scanner.application.service;

import com.ambiental.iga_scanner.domain.RetrievedChunk;
import java.util.List;

/** Strategy: one grounding rule the generated answer must satisfy, run in sequence by {@link GroundednessGuard}. */
@FunctionalInterface
interface AnswerGuardrail {
    String apply(String answer, List<RetrievedChunk> chunks, List<String> catalog);
}
