package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.MarkdownSectionOrganizerMessageError.EMPTY_RESULT;

import com.ambiental.iga_scanner.application.port.out.ChatGenerationPort;
import com.ambiental.iga_scanner.domain.GeneratedAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends the verbatim Markdown produced from a PDF through the same LLM used for chat
 * ({@link ChatGenerationPort}) so it gets reorganized into logical sections and tagged for
 * better retrieval, without summarizing or dropping any information. The LLM's output is
 * expected to follow a strict, parseable section format (see {@link #SYSTEM_PROMPT}), which
 * {@link FixedMarkdownParser} later splits into per-section {@code Document}s.
 */
@Component
class MarkdownSectionOrganizer {

    private static final Logger log = LoggerFactory.getLogger(MarkdownSectionOrganizer.class);

    private static final String SYSTEM_PROMPT = """
            Eres un asistente que reorganiza documentos Markdown extra\u00eddos de un PDF. Este es \
            un requisito de negocio cr\u00edtico: el documento resultante debe ser 100% fiel a la \
            fuente original.

            Reglas estrictas e innegociables:
            1. PROHIBIDO resumir. No acortes, condenses ni sintetices ninguna parte del contenido.
            2. PROHIBIDO reescribir o parafrasear. El texto de cada secci\u00f3n debe ser una copia \
            literal (verbatim) del texto original, car\u00e1cter por car\u00e1cter en su redacci\u00f3n; \
            \u00fanicamente puedes MOVER/REORDENAR fragmentos completos entre secciones, nunca \
            reformularlos.
            3. PROHIBIDO completar, inferir o inventar datos que no est\u00e9n expl\u00edcitamente en \
            el original. Si un dato, valor o campo falta o est\u00e1 incompleto en la fuente, d\u00e9jalo \
            exactamente as\u00ed (vac\u00edo, incompleto o ausente) en el resultado. NUNCA rellenes \
            huecos ni asumas valores.
            4. No omitas ning\u00fan fragmento de texto, tabla, n\u00famero o secci\u00f3n del original: \
            todo el contenido debe aparecer en alguna secci\u00f3n del resultado, sin excepci\u00f3n.
            5. Agrupa el contenido en secciones l\u00f3gicas segun su tema (por ejemplo: datos \
            generales, requisitos, procedimientos, anexos, tablas, etc.), sin importar en qu\u00e9 \
            p\u00e1gina del PDF original aparec\u00edan. Reorganizar el ORDEN est\u00e1 permitido; \
            alterar el CONTENIDO no lo est\u00e1.
            6. Devuelve \u00danicamente el documento completo usando EXACTAMENTE este formato, \
            repetido para cada secci\u00f3n, sin texto adicional antes, entre o despu\u00e9s:

            ## Secci\u00f3n: <t\u00edtulo breve de la secci\u00f3n>
            Tags: <tag1, tag2, tag3>

            <contenido literal y completo de la secci\u00f3n, sin resumir ni reescribir>

            7. Los tags deben ser palabras o frases cortas que ayuden a buscar ese contenido \
            despu\u00e9s (temas, entidades, tipos de dato); los tags son la \u00fanica parte que \
            puedes redactar t\u00fa, nunca el contenido de la secci\u00f3n.
            8. No agregues encabezados, comentarios ni bloques de c\u00f3digo que no formen parte \
            del contenido original, salvo el formato de secci\u00f3n exigido arriba.
            9. LIMPIEZA DE FORMATO (esto S\u00cd est\u00e1 permitido y es obligatorio, no cuenta como \
            reescribir): el markdown de origen viene de una extracci\u00f3n autom\u00e1tica de PDF y \
            trae ruido puramente visual que debes eliminar:
               - Quita bloques de c\u00f3digo (```) usados solo para forzar alineaci\u00f3n de \
               columnas; el texto que contienen debe quedar como texto/markdown normal.
               - Colapsa espacios y tabulaciones repetidas entre palabras a un solo espacio.
               - Elimina s\u00edmbolos o letras sueltas que sean claramente basura de OCR/extracci\u00f3n \
               (ic\u00f3nos de checkbox, vi\u00f1etas mal decodificadas, caracteres aislados sin sentido \
               como una letra suelta entre dos campos de una tabla) SIEMPRE que no formen parte de una \
               palabra, sigla, valor o dato real.
               - Est\u00e1 PROHIBIDO tocar palabras, n\u00fameros, unidades, siglas, signos de puntuaci\u00f3n \
               o cualquier dato real aunque se vean "raros"; ante la duda de si algo es ruido o dato, \
               consid\u00e9ralo dato real y d\u00e9jalo intacto.
            """;

    private final ChatGenerationPort chatGenerationPort;

    MarkdownSectionOrganizer(ChatGenerationPort chatGenerationPort) {
        this.chatGenerationPort = chatGenerationPort;
    }

    String organize(String fileName, String rawMarkdown) {
        String userPrompt = "Archivo: " + fileName + "\n\n" + rawMarkdown;
        log.info("Sending markdown to the AI classifier: file='{}', system_prompt_chars={}, "
                        + "raw_markdown_chars={}, user_prompt_chars={}",
                fileName, SYSTEM_PROMPT.length(), rawMarkdown == null ? 0 : rawMarkdown.length(), userPrompt.length());

        GeneratedAnswer generatedAnswer = chatGenerationPort.generate(SYSTEM_PROMPT, userPrompt);
        String fixedMarkdown = generatedAnswer.answer();
        log.info("AI classifier response for file='{}': input_tokens={}, output_tokens={}, "
                        + "fixed_markdown_chars={}",
                fileName, generatedAnswer.inputTokens(), generatedAnswer.outputTokens(),
                fixedMarkdown == null ? 0 : fixedMarkdown.length());

        if (fixedMarkdown == null || fixedMarkdown.isBlank()) {
            log.error("AI classifier returned a blank/null markdown for file='{}' "
                    + "(raw_markdown_chars={}, input_tokens={}, output_tokens={}); aborting upload",
                    fileName, rawMarkdown == null ? 0 : rawMarkdown.length(),
                    generatedAnswer.inputTokens(), generatedAnswer.outputTokens());
            throw new PdfProcessingException(EMPTY_RESULT);
        }
        return fixedMarkdown;
    }
}
