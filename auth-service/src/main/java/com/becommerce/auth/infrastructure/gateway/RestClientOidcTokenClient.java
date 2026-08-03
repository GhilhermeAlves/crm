package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementação do token endpoint do Keycloak via RestClient (Sprints 6.1/6.2):
 * troca de authorization code por tokens e refresh com rotação. Chamadas
 * exclusivamente no servidor, com timeout e sem jamais logar tokens/secrets.
 *
 * <p>Erros de refresh (6.2): {@code REFRESH_TOKEN_INVALID} quando o provedor
 * rejeita o refresh token (ex.: {@code invalid_grant}), {@code REFRESH_FAILED}
 * para outras falhas do endpoint e {@code OIDC_PROVIDER_UNAVAILABLE} para falhas
 * de comunicação/timeout.
 */
@Component
public class RestClientOidcTokenClient implements OidcTokenClient {

    private static final Pattern ERROR_CODE = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");

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
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
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

    @Override
    public TokenResponse refresh(RefreshRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", request.refreshToken());
        form.add("client_id", properties.getClientId());
        if (StringUtils.hasText(properties.getClientSecret())) {
            form.add("client_secret", properties.getClientSecret());
        }

        Map<String, Object> body;
        try {
            body = restClient.post()
                    .uri(properties.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request1, response) -> {
                        throw refreshError(readErrorCode(response), response.getStatusCode().value());
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new OidcGatewayException("OIDC_PROVIDER_UNAVAILABLE", 504,
                        "Tempo esgotado ao renovar os tokens.");
            }
            throw new OidcGatewayException("OIDC_PROVIDER_UNAVAILABLE", 502,
                    "Falha na comunicação com o provedor de identidade.");
        }

        String accessToken = (String) body.get("access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new OidcGatewayException("REFRESH_FAILED", 502,
                    "Resposta inválida do endpoint de token.");
        }

        Number expiresIn = (Number) body.get("expires_in");
        return new TokenResponse(accessToken, (String) body.get("refresh_token"), (String) body.get("id_token"),
                expiresIn == null ? 300 : expiresIn.longValue());
    }

    private OidcGatewayException refreshError(String errorCode, int status) {
        if (errorCode != null && (errorCode.equals("invalid_grant") || errorCode.equals("invalid_token")
                || errorCode.equals("invalid_client") || errorCode.equals("invalid_request"))) {
            return new OidcGatewayException("REFRESH_TOKEN_INVALID", 401,
                    "Refresh token rejeitado pelo provedor de identidade.");
        }
        return new OidcGatewayException("REFRESH_FAILED", 502,
                "Falha ao renovar os tokens no provedor de identidade.");
    }

    private String readErrorCode(ClientHttpResponse response) {
        try {
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = ERROR_CODE.matcher(body);
            return matcher.find() ? matcher.group(1) : null;
        } catch (IOException e) {
            return null;
        }
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
