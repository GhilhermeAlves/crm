package com.becommerce.crm.infrastructure.identity.client;

import com.becommerce.crm.infrastructure.identity.client.dto.ResetCredentialRequest;
import com.becommerce.crm.infrastructure.identity.client.dto.ResolutionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementação HTTP do {@link AuthServiceClient} via {@link RestClient},
 * apontando para o crm-auth-service (rede interna / porta dedicada).
 *
 * <p>O reset de credencial (Sprint 7.4) é autenticado por segredo de serviço
 * ({@code app.auth.identity-layer.internal-api-token}, header
 * {@code X-Internal-Api-Token}) e a nova senha vai apenas no corpo — nunca em
 * URL, header ou log.
 */
@Component
public class HttpAuthServiceClient implements AuthServiceClient {

    private static final String INTERNAL_API_TOKEN_HEADER = "X-Internal-Api-Token";

    private final RestClient restClient;
    private final String internalApiToken;

    public HttpAuthServiceClient(
            @Value("${app.auth.identity-layer.auth-service-url:}") String authServiceUrl,
            @Value("${app.auth.identity-layer.internal-api-token:}") String internalApiToken,
            RestClient.Builder restClientBuilder) {
        this.internalApiToken = internalApiToken;
        this.restClient = (authServiceUrl == null || authServiceUrl.isBlank())
                ? null
                : restClientBuilder.baseUrl(authServiceUrl).build();
    }

    @Override
    public ResolutionResponse currentUser(String jwtToken) {
        if (restClient == null) {
            throw new IllegalStateException(
                    "app.auth.identity-layer.auth-service-url não configurado (AUTH_SERVICE_URL)");
        }
        return restClient.get()
                .uri("/internal/auth/current-user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ResolutionResponse.class);
    }

    @Override
    public void resetPassword(String keycloakSub, String email, String newPassword) {
        if (restClient == null) {
            throw new IllegalStateException(
                    "app.auth.identity-layer.auth-service-url não configurado (AUTH_SERVICE_URL)");
        }
        restClient.post()
                .uri("/internal/auth/reset-password")
                .header(INTERNAL_API_TOKEN_HEADER, internalApiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ResetCredentialRequest(keycloakSub, email, newPassword))
                .retrieve()
                .toBodilessEntity();
    }
}