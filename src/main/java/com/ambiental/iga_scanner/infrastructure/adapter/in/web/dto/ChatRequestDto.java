package com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record ChatRequestDto(
        UUID sessionId,
        @NotBlank String question,
        String user,
        List<UUID> documents) {
}
