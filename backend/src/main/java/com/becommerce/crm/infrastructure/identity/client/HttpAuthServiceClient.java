package com.becommerce.crm.infrastructure.identity.client;

import com.becommerce.crm.infrastructure.identity.client.dto.ResolutionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementação HTTP do {@link AuthServiceClient} via {@link RestClient},
 * apontando para o crm-auth-service (rede interna / porta dedicada).
 */
@Component
public class HttpAuthServiceClient implements AuthServiceClient {

    private final RestClient restClient;

    public HttpAuthServiceClient(
            @Value("${app.auth.identity-layer.auth-service-url:}") String authServiceUrl,
            RestClient.Builder restClientBuilder) {
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
}
