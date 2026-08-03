package com.becommerce.auth.infrastructure.gateway;

/**
 * Lock por sessão de gateway (Sprint 6.3). Adquirido antes da rotação de tokens
 * no refresh para serializar acessos concorrentes à mesma sessão — nunca um lock
 * global. {@link AutoCloseable} permite uso em try-with-resources:
 * {@code try (GatewaySessionLock lock = store.lockFor(token)) { ... }}.
 */
public interface GatewaySessionLock extends AutoCloseable {

    @Override
    void close();
}
