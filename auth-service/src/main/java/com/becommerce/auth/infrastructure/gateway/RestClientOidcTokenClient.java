package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;

/**
 * Implementação da troca de authorization code por tokens via RestClient
 * (token endpoint do Keycloak). A chamada ocorre exclusivamente no servidor,
 * com timeout e sem jamais logar tokens/secrets. O {@code redirect_uri} é
 * sempre o fixo do gateway (paridade com o Keycloak).
 */
@Component
public class RestClientOidcTokenClient implements OidcTokenClient {

    private final RestClient restClient;
    private final OidcGatewayProperties properties;

    public RestClientOidcTokenClient(OidcGatewayProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(properties.getTokenExchangeTimeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public TokenResponse exchange(ExchangeRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", request.code());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("client_id", properties.getClientId());
        if (StringUtils.hasText(properties.getClientSecret())) {
            form.add("client_secret", properties.getClientSecret());
        }
        form.add("code_verifier", request.codeVerifier());

        Map<String, Object> body;
        try {
            body = restClient.post()
                    .uri(properties.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request1, response) -> {
                        throw new OidcGatewayException("TOKEN_EXCHANGE_FAILED",
                                response.getStatusCode().value(),
                                "Falha na troca de código no provedor de identidade.");
                    })
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new OidcGatewayException("TOKEN_EXCHANGE_TIMEOUT", 504,
                        "Tempo esgotado ao trocar o código de autorização.");
            }
            throw new OidcGatewayException("TOKEN_EXCHANGE_FAILED", 502,
                    "Falha na comunicação com o provedor de identidade.");
        }

        String accessToken = (String) body.get("access_token");
        String idToken = (String) body.get("id_token");
        if (accessToken == null || accessToken.isBlank() || idToken == null || idToken.isBlank()) {
            throw new OidcGatewayException("TOKEN_RESPONSE_INVALID", 502,
                    "Resposta inválida do endpoint de token.");
        }

        Number expiresIn = (Number) body.get("expires_in");
        return new TokenResponse(accessToken, (String) body.get("refresh_token"), idToken,
                expiresIn == null ? 300 : expiresIn.longValue());
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
