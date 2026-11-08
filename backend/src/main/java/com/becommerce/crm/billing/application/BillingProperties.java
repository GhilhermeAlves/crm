package com.becommerce.crm.billing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.billing")
public record BillingProperties(boolean enabled, String baseUrl, String apiKey, String webhookToken) {
    public boolean configured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
