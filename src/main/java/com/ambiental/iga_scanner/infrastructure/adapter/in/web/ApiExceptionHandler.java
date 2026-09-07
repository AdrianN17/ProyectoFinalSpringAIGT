package com.ambiental.iga_scanner.infrastructure.adapter.in.web;

import static com.ambiental.iga_scanner.infrastructure.adapter.in.web.ApiExceptionHandlerMessageError.INVALID_REQUEST;

import com.ambiental.iga_scanner.application.exception.InvalidCommandException;
import com.ambiental.iga_scanner.infrastructure.adapter.out.ai.AiGenerationException;
import com.ambiental.iga_scanner.infrastructure.adapter.out.ai.RetrievalFailedException;
import com.ambiental.iga_scanner.infrastructure.adapter.out.pdf.PdfProcessingException;
import com.ambiental.iga_scanner.infrastructure.adapter.out.persistence.PersistenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiExceptionHandler {

    // Application layer: a use-case command failed its own input validation.
    @ExceptionHandler(InvalidCommandException.class)
    ProblemDetail handleInvalidCommand(InvalidCommandException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Web adapter layer: the multipart upload itself could not be read.
    @ExceptionHandler(FileReadException.class)
    ProblemDetail handleFileRead(FileReadException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
    }

    // AI adapter layer: the upstream model/vector-store call failed.
    @ExceptionHandler({AiGenerationException.class, RetrievalFailedException.class})
    ProblemDetail handleAiFailure(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    // PDF and persistence adapter layers: a local processing/storage failure occurred.
    @ExceptionHandler({PdfProcessingException.class, PersistenceException.class})
    ProblemDetail handleInfrastructureFailure(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
}
