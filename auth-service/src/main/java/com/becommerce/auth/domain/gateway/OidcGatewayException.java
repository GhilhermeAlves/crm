package com.becommerce.auth.domain.gateway;

/**
 * Falha no fluxo OIDC do Access Gateway (Sprint 6.1). Carrega um {@code code}
 * estável e um {@code status} HTTP, no mesmo padrão do projeto
 * ({@code status, code, error, message, timestamp}).
 *
 * <p>Nunca deve carregar tokens, códigos de autorização ou secrets — a mensagem
 * é destinada ao usuário/cliente e não pode vazar material sensível.
 */
public class OidcGatewayException extends RuntimeException {

    private final String code;
    private final int status;

    public OidcGatewayException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
