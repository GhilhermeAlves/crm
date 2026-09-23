package com.becommerce.crm.infrastructure.security.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra o resolver central de tenant para toda a aplicação, permitindo que os
 * controllers declarem {@code @CurrentCompanyId} no parâmetro {@code companyId}
 * do path em vez de repetir a checagem {@code requireCompanyAccess}.
 */
@Configuration
public class CurrentCompanyIdWebConfig implements WebMvcConfigurer {

    private final CurrentCompanyIdArgumentResolver currentCompanyIdArgumentResolver;

    public CurrentCompanyIdWebConfig(CurrentCompanyIdArgumentResolver currentCompanyIdArgumentResolver) {
        this.currentCompanyIdArgumentResolver = currentCompanyIdArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentCompanyIdArgumentResolver);
    }
}