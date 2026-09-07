package com.ambiental.iga_scanner.application.port.in;

import static com.ambiental.iga_scanner.application.port.in.ChatCommandMessageError.BLANK_QUESTION;

import com.ambiental.iga_scanner.application.exception.InvalidCommandException;
import java.util.UUID;

public record ChatCommand(UUID sessionId, String question, String user) {

    public ChatCommand {
        if (question == null || question.isBlank()) {
            throw new InvalidCommandException(BLANK_QUESTION);
        }
    }
}
