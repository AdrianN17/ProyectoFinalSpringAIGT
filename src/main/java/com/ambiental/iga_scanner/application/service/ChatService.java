package com.ambiental.iga_scanner.application.service;

import static com.ambiental.iga_scanner.application.service.GroundednessGuardMessageError.NOT_FOUND_ANSWER;

import com.ambiental.iga_scanner.application.port.in.AskChatUseCase;
import com.ambiental.iga_scanner.application.port.in.ChatCommand;
import com.ambiental.iga_scanner.application.port.out.ChatGenerationPort;
import com.ambiental.iga_scanner.application.port.out.ChatRetrievalPort;
import com.ambiental.iga_scanner.application.port.out.DocumentCatalogPort;
import com.ambiental.iga_scanner.application.port.out.HistoryRepositoryPort;
import com.ambiental.iga_scanner.domain.GeneratedAnswer;
import com.ambiental.iga_scanner.domain.HistoryTrace;
import com.ambiental.iga_scanner.domain.RetrievedChunk;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatService implements AskChatUseCase {

    private static final int TOP_K = 8;

    // Business rules: verbatim, source-grounded answers only; never fall back to general knowledge.
    private static final String SYSTEM_PROMPT = """
            Eres un asistente que responde preguntas sobre compromisos ambientales a partir \
            de documentos PDF ingeridos. Reglas de negocio obligatorias:
            0. Tu unica fuente de verdad es el contexto recuperado de la base vectorial Qdrant y \
            el catalogo de documentos que se te dan en este mensaje. Ignora por completo cualquier \
            conocimiento propio, de entrenamiento o de internet: nunca respondas con informacion \
            que no provenga literalmente de ese contexto, aunque creas saber la respuesta.
            1. NUNCA resumas ni parafrasees el contenido de los documentos. Cita el texto tal \
            cual aparece en el contexto recuperado, palabra por palabra.
            2. Toda afirmacion basada en un documento debe ir acompanada de su cita de fuente \
            con el formato [Fuente: <nombre-del-documento>], usando exactamente un nombre del \
            catalogo o de los fragmentos recuperados, nunca un nombre inventado.
            3. Si te preguntan que documentos o compromisos ambientales existen, responde \
            unicamente con el listado de nombres de documentos del catalogo provisto, sin inventar ni omitir.
            4. Si el contexto recuperado no contiene informacion suficiente para responder \
            textualmente, responde exactamente: "%s". No completes con conocimiento propio.
            """.formatted(NOT_FOUND_ANSWER);

    private final ChatRetrievalPort chatRetrievalPort;
    private final ChatGenerationPort chatGenerationPort;
    private final HistoryRepositoryPort historyRepositoryPort;
    private final DocumentCatalogPort documentCatalogPort;

    public ChatService(ChatRetrievalPort chatRetrievalPort,
            ChatGenerationPort chatGenerationPort,
            HistoryRepositoryPort historyRepositoryPort,
            DocumentCatalogPort documentCatalogPort) {
        this.chatRetrievalPort = chatRetrievalPort;
        this.chatGenerationPort = chatGenerationPort;
        this.historyRepositoryPort = historyRepositoryPort;
        this.documentCatalogPort = documentCatalogPort;
    }

    @Override
    public HistoryTrace ask(ChatCommand command) {
        UUID sessionId = command.sessionId() != null ? command.sessionId() : UUID.randomUUID();
        List<RetrievedChunk> retrievedChunks =
                chatRetrievalPort.retrieve(command.question(), TOP_K, command.documents());
        List<String> catalog = documentCatalogPort.listDocumentNames();

        String answer;
        Integer inputTokens = null;
        Integer outputTokens = null;
        if (!GroundednessGuard.hasContext(retrievedChunks, catalog)) {
            // Guardrail: with nothing to ground on, never call the model at all.
            answer = NOT_FOUND_ANSWER;
        } else {
            String userPrompt = buildUserPrompt(command.question(), retrievedChunks, catalog);
            GeneratedAnswer generatedAnswer = chatGenerationPort.generate(SYSTEM_PROMPT, userPrompt);
            answer = GroundednessGuard.enforce(generatedAnswer.answer(), retrievedChunks, catalog);
            inputTokens = generatedAnswer.inputTokens();
            outputTokens = generatedAnswer.outputTokens();
        }

        HistoryTrace trace = HistoryTrace.of(
                sessionId,
                command.question(),
                answer,
                command.user(),
                inputTokens,
                outputTokens,
                serializeContexts(retrievedChunks));

        return historyRepositoryPort.save(trace);
    }

    private String buildUserPrompt(String question, List<RetrievedChunk> chunks, List<String> catalog) {
        String catalogSection = catalog.isEmpty()
                ? "(sin documentos ingeridos todavia)"
                : catalog.stream().map(name -> "- " + name).collect(Collectors.joining("\n"));

        String contextSection = chunks.isEmpty()
                ? "(sin fragmentos relevantes encontrados)"
                : chunks.stream()
                        .map(chunk -> "[Fuente: %s%s]\n\"\"\"\n%s\n\"\"\"".formatted(
                                chunk.sourceDocument(),
                                chunk.page() != null ? ", pagina " + chunk.page() : "",
                                chunk.content()))
                        .collect(Collectors.joining("\n\n"));

        return """
                Catalogo de documentos ingeridos:
                %s

                Fragmentos recuperados (usalos textualmente, no los resumas):
                %s

                Pregunta del usuario:
                %s
                """.formatted(catalogSection, contextSection, question);
    }

    private String serializeContexts(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(chunk -> "[%s%s] %s".formatted(
                        chunk.sourceDocument(),
                        chunk.page() != null ? ":" + chunk.page() : "",
                        chunk.content()))
                .collect(Collectors.joining("\n---\n"));
    }
}
