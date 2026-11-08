package com.becommerce.crm.billing.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BillingGuardWebConfig implements WebMvcConfigurer {

    private final BillingGuardInterceptor guard;

    public BillingGuardWebConfig(BillingGuardInterceptor guard) { this.guard = guard; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(guard).addPathPatterns("/api/v1/**");
    }
}
