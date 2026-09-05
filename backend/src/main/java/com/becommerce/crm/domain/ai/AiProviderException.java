package com.becommerce.crm.domain.ai;

/**
 * Falha na chamada ao provider de IA (rede, resposta inesperada, ausência de
 * API key, etc.). Não expõe detalhes internos/segredos ao cliente.
 *
 * <p>Sprint 2: {@link #isRecoverable()} classifica o erro para o failover.
 * Erro RECUPERÁVEL (timeout, 429, 5xx, indisponibilidade temporária) pode
 * tentar um provider/modelo fallback. Erro NÃO recuperável (config/chave
 * inválida, request estruturalmente inválido) NÃO provoca fallback. O default
 * (construtores históricos) é não recuperável — conservador para o failover.</p>
 */
public class AiProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean recoverable;

    public AiProviderException(String message) {
        this(message, null, false);
    }

    public AiProviderException(String message, Throwable cause) {
        this(message, cause, false);
    }

    /** Semanticamente recuperável (timeout/temporário/5xx) — candidato a fallback. */
    public AiProviderException(String message, boolean recoverable) {
        this(message, null, recoverable);
    }

    public AiProviderException(String message, Throwable cause, boolean recoverable) {
        super(message, cause);
        this.recoverable = recoverable;
    }

    public boolean isRecoverable() {
        return recoverable;
    }
}