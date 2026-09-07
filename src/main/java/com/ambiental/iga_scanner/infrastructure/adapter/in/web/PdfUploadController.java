package com.ambiental.iga_scanner.infrastructure.adapter.in.web;

import static com.ambiental.iga_scanner.infrastructure.adapter.in.web.PdfUploadControllerMessageError.EMPTY_FILE;
import static com.ambiental.iga_scanner.infrastructure.adapter.in.web.PdfUploadControllerMessageError.FILE_READ_ERROR;
import static com.ambiental.iga_scanner.infrastructure.adapter.in.web.PdfUploadControllerMessageError.UNSUPPORTED_MEDIA_TYPE;

import com.ambiental.iga_scanner.application.port.in.UploadPdfCommand;
import com.ambiental.iga_scanner.application.port.in.UploadPdfUseCase;
import com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto.PdfUploadResponseDto;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
class PdfUploadController {

    private final UploadPdfUseCase uploadPdfUseCase;

    PdfUploadController(UploadPdfUseCase uploadPdfUseCase) {
        this.uploadPdfUseCase = uploadPdfUseCase;
    }

    @PostMapping(path = "/api/pdf/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    PdfUploadResponseDto upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, EMPTY_FILE);
        }
        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE);
        }
        try {
            var document = uploadPdfUseCase.upload(new UploadPdfCommand(file.getOriginalFilename(), file.getBytes()));
            return PdfUploadResponseDto.from(document);
        } catch (IOException e) {
            throw new FileReadException(FILE_READ_ERROR, e);
        }
    }
}
