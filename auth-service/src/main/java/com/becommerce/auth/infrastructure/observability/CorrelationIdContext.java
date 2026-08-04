package com.becommerce.auth.infrastructure.observability;

/**
 * Contexto do {@code Correlation ID} da requisição atual (Sprint 6.6).
 *
 * <p>O valor é preenchido pelo {@link CorrelationIdFilter} (a partir do header
 * {@code X-Correlation-Id} validado ou gerado no servidor) e fica disponível
 * durante todo o processamento da requisição: logging (via MDC), relay e
 * respostas de erro. É limpo ao final do filtro — nunca vaza entre requisições
 * nem threads do pool.
 */
public final class CorrelationIdContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationIdContext() {
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
