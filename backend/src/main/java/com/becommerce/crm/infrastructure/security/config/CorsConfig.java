package com.becommerce.crm.infrastructure.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração CORS declarativa.
 *
 * <p>Default restrito: sem {@code app.cors.allowed-origins} configurado, NENHUMA
 * origem cross-origin é aceita — não há mais fallback silencioso para {@code *}.
 * O perfil {@code dev} libera {@code http://localhost:3000} (frontend Next local)
 * e qualquer ambiente pode sobrescrever via {@code CORS_ALLOWED_ORIGINS} (ou
 * {@code APP_CORS_ALLOWED_ORIGINS}, via relaxed binding do Spring).
 *
 * <p>Em produção, ausência de origens derruba a inicialização em vez de operar
 * com uma política indefinida: o operador precisa declarar explicitamente a
 * allowlist. O frontend de produção é servido no mesmo domínio (nginx roteia
 * {@code /api} e {@code /auth}), então não depende de CORS.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> ALLOWED_HEADERS = List.of("*");
    private static final List<String> EXPOSED_HEADERS = List.of("Authorization");

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") String allowedOrigins,
                      Environment environment) {
        this.allowedOrigins = parseOrigins(allowedOrigins);
        if (this.allowedOrigins.isEmpty()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                log.error("CORS sem origens configuradas em produção. Defina CORS_ALLOWED_ORIGINS "
                        + "(ou APP_CORS_ALLOWED_ORIGINS) com a allowlist explícita.");
                throw new IllegalStateException(
                        "app.cors.allowed-origins vazio em produção: defina CORS_ALLOWED_ORIGINS.");
            }
            log.warn("CORS sem origens configuradas: requisições cross-origin serão bloqueadas.");
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setExposedHeaders(EXPOSED_HEADERS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    static List<String> parseOrigins(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
