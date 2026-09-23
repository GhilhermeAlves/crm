package com.becommerce.crm.infrastructure.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trava a configuração CORS: default restrito (sem wildcard), origens
 * explícitas normalizadas e fail-fast em produção quando nada é configurado.
 */
class CorsConfigTest {

    private static CorsConfiguration configurationFor(String origins, String... profiles) {
        StandardEnvironment environment = new StandardEnvironment();
        if (profiles.length > 0) {
            environment.setActiveProfiles(profiles);
        }
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) new CorsConfig(origins, environment).corsConfigurationSource();
        return source.getCorsConfigurations().get("/**");
    }

    @Test
    void devDefault_permiteApenasLocalhost3000() {
        CorsConfiguration config = configurationFor("http://localhost:3000");

        assertEquals(List.of("http://localhost:3000"), config.getAllowedOrigins());
        assertNull(config.getAllowedOriginPatterns());
    }

    @Test
    void semOrigemConfigurada_foraDeProducao_naoPermiteNenhumaOrigem() {
        CorsConfiguration config = configurationFor("");

        assertTrue(config.getAllowedOrigins() == null || config.getAllowedOrigins().isEmpty());
        assertTrue(config.getAllowedOriginPatterns() == null || config.getAllowedOriginPatterns().isEmpty());
    }

    @Test
    void semOrigemConfigurada_emProducao_falhaComMensagemClara() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> configurationFor("", "prod"));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("CORS_ALLOWED_ORIGINS"),
                "a mensagem deve apontar a variável a configurar: " + ex.getMessage());
    }

    @Test
    void envCustom_sobrescreveDefault() {
        CorsConfiguration config = configurationFor("https://app.exemplo.com,http://localhost:3000");

        assertEquals(List.of("https://app.exemplo.com", "http://localhost:3000"), config.getAllowedOrigins());
    }

    @Test
    void origensComEspacosEVazios_saoNormalizadas() {
        CorsConfiguration config = configurationFor(" https://app.exemplo.com , ,http://localhost:3000 ");

        assertEquals(List.of("https://app.exemplo.com", "http://localhost:3000"), config.getAllowedOrigins());
    }

    @Test
    void devProfileDefault_noApplicationYml_incluiFrontendLocal() throws Exception {
        PropertySource<?> source = new YamlPropertySourceLoader()
                .load("application-dev.yml", new ClassPathResource("application-dev.yml"))
                .get(0);
        String raw = (String) source.getProperty("app.cors.allowed-origins");

        assertNotNull(raw, "app.cors.allowed-origins deve existir no perfil dev");
        assertNotEquals("*", raw, "o default não pode ser wildcard");
        assertTrue(raw.contains("http://localhost:3000"),
                "o default de dev deve permitir o frontend local: " + raw);
    }
}
