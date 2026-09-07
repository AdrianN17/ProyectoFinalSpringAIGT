package com.ambiental.iga_scanner.application.port.out;

import com.ambiental.iga_scanner.domain.GeneratedAnswer;

@FunctionalInterface
public interface ChatGenerationPort {
    GeneratedAnswer generate(String systemPrompt, String userPrompt);
}
