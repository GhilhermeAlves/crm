package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

/**
 * Client HTTP para a API interna de identidade do crm-backend (Sprint 7.2):
 *
 * <ul>
 *   <li>{@code POST /internal/auth/provision} — Caso C (auto-provision de
 *       identidade externa sem conta CRM);</li>
 *   <li>{@code POST /internal/auth/link} — Caso B (vínculo da identidade
 *       externa à conta local após verificação da senha).</li>
 * </ul>
 *
 * <p>Ambas as chamadas carregam o <b>access token do próprio usuário</b>
 * (bearer), que o backend valida contra o JWKS do Keycloak. Rede interna:
 * nunca expõe endpoints, tokens ou a senha ao browser.
 */
@Component
public class BackendIdentityClient {

    private final OidcGatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public BackendIdentityClient(OidcGatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getApiConnectTimeout());
        requestFactory.setReadTimeout(properties.getApiReadTimeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public ProvisionOutcome provision(String accessToken) {
        try {
            Map<String, Object> body = post("/internal/auth/provision", accessToken, Map.of());
            if ("PROVISIONED".equals(body.get("status"))) {
                return ProvisionOutcome.PROVISIONED;
            }
            throw new OidcGatewayException("BACKEND_IDENTITY_INVALID", 502,
                    "Resposta inesperada do backend ao provisionar a identidade.");
        } catch (BackendIdentityHttpException e) {
            if ("LINKING_REQUIRED".equals(e.getBackendStatus())) {
                return ProvisionOutcome.LINKING_REQUIRED;
            }
            throw e;
        }
    }

    public LinkOutcome link(String accessToken, String password) {
        try {
            Map<String, Object> body = post("/internal/auth/link", accessToken, Map.of("password", password));
            if ("LINKED".equals(body.get("status"))) {
                return LinkOutcome.LINKED;
            }
            throw new OidcGatewayException("BACKEND_IDENTITY_INVALID", 502,
                    "Resposta inesperada do backend ao vincular a identidade.");
        } catch (BackendIdentityHttpException e) {
            return switch (e.getBackendStatus()) {
                case "INVALID_CREDENTIALS" -> LinkOutcome.INVALID_CREDENTIALS;
                case "LINK_NOT_FOUND" -> LinkOutcome.LINK_NOT_FOUND;
                default -> throw e;
            };
        }
    }

    private Map<String, Object> post(String path, String accessToken, Map<String, Object> payload) {
        try {
            return restClient.post()
                    .uri(properties.getApiBackendUrl() + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(status -> status.value() == 401 || status.value() == 404 || status.value() == 409,
                            (request, response) -> {
                                throw new BackendIdentityHttpException(readStatus(response), response.getStatusCode().value());
                            })
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new OidcGatewayException("BACKEND_IDENTITY_UNAVAILABLE",
                                response.getStatusCode().value(),
                                "Falha na comunicação com o backend de identidade.");
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (BackendIdentityHttpException e) {
            throw e;
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private String readStatus(ClientHttpResponse response) {
        try {
            Map<String, Object> body = objectMapper.readValue(
                    response.getBody().readAllBytes(), new TypeReference<Map<String, Object>>() {
                    });
            Object status = body.get("status");
            return status instanceof String s ? s : "";
        } catch (IOException e) {
            return "";
        }
    }

    private OidcGatewayException unavailable(RestClientException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return new OidcGatewayException("BACKEND_IDENTITY_TIMEOUT", 504,
                        "Tempo esgotado ao falar com o backend de identidade.");
            }
            cause = cause.getCause();
        }
        return new OidcGatewayException("BACKEND_IDENTITY_UNAVAILABLE", 502,
                "Falha na comunicação com o backend de identidade.");
    }

    /**
     * Resposta esperada de erro do backend (401 INVALID_CREDENTIALS /
     * 404 LINK_NOT_FOUND / 409 LINKING_REQUIRED) com o {@code status} interno
     * preservado para decisão do fluxo.
     */
    public static class BackendIdentityHttpException extends OidcGatewayException {
        private final String backendStatus;

        BackendIdentityHttpException(String backendStatus, int backendHttpStatus) {
            super(backendStatus, backendHttpStatus, backendStatus);
            this.backendStatus = backendStatus;
        }

        public String getBackendStatus() {
            return backendStatus;
        }
    }

    public enum ProvisionOutcome {
        PROVISIONED, LINKING_REQUIRED
    }

    public enum LinkOutcome {
        LINKED, INVALID_CREDENTIALS, LINK_NOT_FOUND
    }
}
