package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Descoberta dos metadados OIDC do provedor ({@code /.well-known/
 * openid-configuration}), usada para obter o {@code end_session_endpoint} no
 * logout (Sprint 6.2). O resultado é cacheado por um TTL curto para evitar uma
 * chamada de descoberta por logout.
 *
 * <p>Falha de descoberta/indisponibilidade resulta em
 * {@link OidcGatewayException} com código {@code OIDC_PROVIDER_UNAVAILABLE}.
 */
@Component
public class OidcProviderMetadata {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final OidcGatewayProperties properties;
    private final RestClient restClient;

    private volatile String endSessionEndpoint;
    private volatile Instant cachedAt;

    public OidcProviderMetadata(OidcGatewayProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(properties.getTokenExchangeTimeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public String endSessionEndpoint() {
        String cached = endSessionEndpoint;
        if (cached != null && isFresh(cachedAt)) {
            return cached;
        }
        return refreshCache();
    }

    /**
     * Sonda o provedor com uma descoberta OIDC <b>fresca</b> (ignora o cache de
     * 5m) e sem lançar exceção — usado pelo readiness (Sprint 6.6) para
     * detectar rapidamente a indisponibilidade/recuperação do Keycloak sem
     * compartilhar outro mecanismo de discovery.
     */
    public boolean isReachable() {
        try {
            fetchDiscovery();
            return true;
        } catch (OidcGatewayException e) {
            return false;
        }
    }

    private synchronized String refreshCache() {
        String cached = endSessionEndpoint;
        if (cached != null && isFresh(cachedAt)) {
            return cached;
        }
        Map<String, Object> metadata = fetchDiscovery();
        Object endpoint = metadata == null ? null : metadata.get("end_session_endpoint");
        if (endpoint == null || !(endpoint instanceof String value) || value.isBlank()) {
            throw unavailable();
        }
        endSessionEndpoint = value;
        cachedAt = Instant.now();
        return value;
    }

    private Map<String, Object> fetchDiscovery() {
        String discoveryUri = properties.getIssuerUri() + "/.well-known/openid-configuration";
        try {
            return restClient.get()
                    .uri(discoveryUri)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw unavailable();
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private boolean isFresh(Instant cachedAt) {
        return cachedAt != null && !cachedAt.plus(CACHE_TTL).isBefore(Instant.now());
    }

    private OidcGatewayException unavailable() {
        return new OidcGatewayException("OIDC_PROVIDER_UNAVAILABLE", 502,
                "Provedor de identidade indisponível.");
    }
}
