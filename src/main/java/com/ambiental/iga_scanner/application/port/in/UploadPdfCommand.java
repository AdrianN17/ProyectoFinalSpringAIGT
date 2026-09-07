package com.ambiental.iga_scanner.application.port.in;

import static com.ambiental.iga_scanner.application.port.in.UploadPdfCommandMessageError.BLANK_FILE_NAME;
import static com.ambiental.iga_scanner.application.port.in.UploadPdfCommandMessageError.EMPTY_FILE_CONTENT;

import com.ambiental.iga_scanner.application.exception.InvalidCommandException;

public record UploadPdfCommand(String fileName, byte[] content) {

    public UploadPdfCommand {
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidCommandException(BLANK_FILE_NAME);
        }
        if (content == null || content.length == 0) {
            throw new InvalidCommandException(EMPTY_FILE_CONTENT);
        }
    }
}
