package com.ambiental.iga_scanner.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the bundled web UI ({@code /chat.html}) to call the REST API even when it is opened
 * from a different local origin than the Spring Boot server (e.g. an IDE's built-in preview
 * server, or a static file server), where browsers would otherwise block the requests as
 * cross-origin. Restricted to {@code /api/**} only.
 */
@Configuration
class WebCorsConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
