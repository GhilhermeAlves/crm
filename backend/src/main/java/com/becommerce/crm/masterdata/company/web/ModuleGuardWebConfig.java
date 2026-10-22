package com.becommerce.crm.masterdata.company.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ModuleGuardWebConfig implements WebMvcConfigurer {

    private final ModuleGuardInterceptor moduleGuardInterceptor;

    public ModuleGuardWebConfig(ModuleGuardInterceptor moduleGuardInterceptor) {
        this.moduleGuardInterceptor = moduleGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moduleGuardInterceptor)
                .addPathPatterns("/api/v1/companies/**");
    }
}
