package com.becommerce.auth.infrastructure.identity.client;

import com.becommerce.auth.application.identity.port.output.KeycloakAuthPort;
import com.becommerce.auth.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.auth.domain.identity.exception.KeycloakUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Profile;

@Component
@Profile("!test")
public class KeycloakAdminClientImpl implements KeycloakAuthPort {

    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    public KeycloakAdminClientImpl(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${app.keycloak.realm:master}") String realm,
            @Value("${app.keycloak.client-id:}") String clientId,
            @Value("${app.keycloak.client-secret:}") String clientSecret,
            RestClient.Builder restClientBuilder) {
        this.serverUrl = issuerUri.replaceAll("/realms/.*$", "");
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = restClientBuilder.baseUrl(this.serverUrl).build();
    }

    @Override
    public String authenticateWithCredentials(String email, String password) {
        try {
            var response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=password" +
                            "&client_id=" + clientId +
                            "&client_secret=" + clientSecret +
                            "&username=" + email +
                            "&password=" + password +
                            "&scope=openid profile email")
                    .retrieve()
                    .onStatus(status -> status.value() == 401,
                            (request, errorResponse) -> {
                                throw new InvalidCredentialsException("Email ou senha inválidos.");
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            (request, errorResponse) -> {
                                throw new KeycloakUnavailableException(
                                    "Keycloak temporariamente indisponível", null);
                            })
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.access_token == null) {
                throw new KeycloakUnavailableException(
                    "Resposta inválida do Keycloak", null);
            }

            return response.access_token;
        } catch (Exception e) {
            if (e instanceof InvalidCredentialsException ||
                e instanceof KeycloakUnavailableException) {
                throw e;
            }
            throw new KeycloakUnavailableException(
                "Erro ao autenticar com Keycloak: " + e.getMessage(), e);
        }
    }

    public static class KeycloakTokenResponse {
        public String access_token;
        public String token_type;
        public int expires_in;
        public String refresh_token;
    }
}
