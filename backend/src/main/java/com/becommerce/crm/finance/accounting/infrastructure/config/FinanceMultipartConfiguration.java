package com.becommerce.crm.finance.accounting.infrastructure.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Limite de upload para a importação CSV de lançamentos (Etapa 3). O Spring
 * Boot padrão é 1MB (o que viraria 500 via handler genérico); aqui sobe para
 * 5MB e o {@code MaxUploadSizeExceededException} é mapeado para 400 no
 * {@code GlobalExceptionHandler}.
 */
@Configuration
public class FinanceMultipartConfiguration {

    private static final long FIVE_MB = 5L * 1024 * 1024;
    private static final long SIX_MB = 6L * 1024 * 1024;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", FIVE_MB, SIX_MB, (int) FIVE_MB);
    }
}