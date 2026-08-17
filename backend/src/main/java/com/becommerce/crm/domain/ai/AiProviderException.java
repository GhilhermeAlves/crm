package com.becommerce.crm.domain.ai;

/**
 * Falha na chamada ao provider de IA (rede, resposta inesperada, ausência de
 * API key, etc.). Não expõe detalhes internos/segredos ao cliente.
 */
public class AiProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}