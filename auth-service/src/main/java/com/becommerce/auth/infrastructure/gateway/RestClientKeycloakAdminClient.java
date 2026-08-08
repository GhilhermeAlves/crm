package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.application.gateway.port.output.CredentialResetClient;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;

/**
 * Implementação do {@link CredentialResetClient} via Admin REST do Keycloak
 * (Sprint 7.4) — {@link RestClient}. Fluxo:
 *
 * <ol>
 *   <li>Token de admin: {@code client_credentials} de um client confidencial de
 *       ADMIN (ex.: {@code crm-keycloak-admin}) no realm, via RestClient;</li>
 *   <li>Resolve o usuário: usa {@code sub} como id diretamente se informado, ou
 *       busca exata por e-mail em {@code GET /admin/realms/{realm}/users?email=..};</li>
 *   <li>Redefine via {@code PUT /admin/realms/{realm}/users/{id}/reset-password}
 *       com {@code {type:password, value:..., temporary:false}}.</li>
 * </ol>
 *
 * <p>A nova senha é enviada somente no corpo da requisição, nunca logada. Erros
 * de comunicação são mapeados para {@link OidcGatewayException}.
 */
@Component
public class RestClientKeycloakAdminClient implements CredentialResetClient {

    private static final String SUCCESS = "success";

    private final KeycloakAdminProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public RestClientKeycloakAdminClient(KeycloakAdminProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(properties.getTokenTimeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public void resetPassword(String keycloakSub, String email, String newPassword) {
        if (!properties.isConfigured()) {
            throw new OidcGatewayException("KEYCLOAK_ADMIN_NOT_CONFIGURED", 503,
                    "AUTH_KEYCLOAK_ADMIN_CLIENT_ID/SECRET não configurado.");
        }

        String adminToken = adminToken();
        String userId = resolveUserId(adminToken, keycloakSub, email);
        putResetPassword(adminToken, userId, newPassword);
    }

    private String adminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        Map<String, Object> body;
        try {
            body = restClient.post()
                    .uri(tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new OidcGatewayException("KEYCLOAK_ADMIN_TIMEOUT", 504,
                        "Tempo esgotado ao obter token de service account do Keycloak.");
            }
            throw new OidcGatewayException("KEYCLOAK_ADMIN_UNAVAILABLE", 502,
                    "Falha na comunicação com o Keycloak (service account).");
        }

        if (body == null || body.get("access_token") == null || ((String) body.get("access_token")).isBlank()) {
            throw new OidcGatewayException("KEYCLOAK_ADMIN_TOKEN_INVALID", 401,
                    "Credenciais de service account rejeitadas pelo Keycloak.");
        }
        return (String) body.get("access_token");
    }

    private String resolveUserId(String adminToken, String keycloakSub, String email) {
        if (keycloakSub != null && !keycloakSub.isBlank()) {
            return keycloakSub;
        }
        if (email == null || email.isBlank()) {
            throw new OidcGatewayException("RESET_USER_UNRESOLVED", 400,
                    "Identidade do usuário ausente para reset de senha.");
        }

        JsonNode body;
        try {
            String raw = restClient.get()
                    .uri(properties.getBaseUrl() + "/admin/realms/{realm}/users?email={email}&exact=true",
                            properties.getRealm(), email)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new OidcGatewayException("KEYCLOAK_ADMIN_USER_LOOKUP_FAILED",
                                response.getStatusCode().value(),
                                "Falha ao localizar o usuário no Keycloak.");
                    })
                    .body(String.class);
            body = objectMapper.readTree(raw);
        } catch (IOException e) {
            throw new OidcGatewayException("KEYCLOAK_ADMIN_BAD_RESPONSE", 502,
                    "Resposta inválida do Keycloak ao buscar usuário.");
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new OidcGatewayException("KEYCLOAK_ADMIN_TIMEOUT", 504,
                        "Tempo esgotado ao buscar usuário no Keycloak.");
            }
            throw new OidcGatewayException("KEYCLOAK_ADMIN_UNAVAILABLE", 502,
                    "Falha na comunicação com o Keycloak (busca de usuário).");
        }

        if (body == null || !body.isArray() || body.isEmpty()) {
            throw new OidcGatewayException("RESET_USER_NOT_FOUND", 404,
                    "Nenhum usuário do Keycloak com o e-mail informado.");
        }
        JsonNode id = body.get(0).get("id");
        if (id == null || id.isNull() || id.asText().isBlank()) {
            throw new OidcGatewayException("KEYCLOAK_ADMIN_BAD_RESPONSE", 502,
                    "Usuário do Keycloak sem id na resposta.");
        }
        return id.asText();
    }

    private void putResetPassword(String adminToken, String userId, String newPassword) {
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );
        try {
            restClient.put()
                    .uri(properties.getBaseUrl() + "/admin/realms/{realm}/users/{id}/reset-password",
                            properties.getRealm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credential)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new OidcGatewayException("KEYCLOAK_RESET_PASSWORD_FAILED",
                                response.getStatusCode().value(),
                                "O Keycloak rejeitou a redefinição de senha.");
                    })
                    .toBodilessEntity();
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new OidcGatewayException("KEYCLOAK_ADMIN_TIMEOUT", 504,
                        "Tempo esgotado ao redefinir a senha no Keycloak.");
            }
            throw new OidcGatewayException("KEYCLOAK_RESET_PASSWORD_UNAVAILABLE", 502,
                    "Falha na comunicação com o Keycloak ao redefinir a senha.");
        }
    }

    private String tokenEndpoint() {
        return properties.getBaseUrl() + "/realms/" + properties.getRealm() + "/protocol/openid-connect/token";
    }

    private boolean isTimeout(RestClientException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}