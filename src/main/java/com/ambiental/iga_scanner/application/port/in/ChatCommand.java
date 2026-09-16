package com.ambiental.iga_scanner.application.port.in;

import static com.ambiental.iga_scanner.application.port.in.ChatCommandMessageError.BLANK_QUESTION;

import com.ambiental.iga_scanner.application.exception.InvalidCommandException;
import java.util.List;
import java.util.UUID;

public record ChatCommand(UUID sessionId, String question, String user, List<UUID> documents) {

    public ChatCommand {
        if (question == null || question.isBlank()) {
            throw new InvalidCommandException(BLANK_QUESTION);
        }
        // Normalize null/omitted filter to "search everything" rather than propagating null.
        documents = documents == null ? List.of() : List.copyOf(documents);
    }
}
