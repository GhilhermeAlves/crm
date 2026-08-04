package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.SessionLookup;
import com.becommerce.auth.domain.gateway.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BFF relay do Access Gateway (Sprint 6.4).
 *
 * <p>Repassa requisições de {@code /api/**} do browser ao crm-backend injetando
 * o access token da sessão de gateway ({@link GatewaySession}) — o único
 * detentor do token permanece o servidor. Fluxo:
 * <ol>
 *   <li>resolve a sessão pelo {@code sessionToken} opaco do cookie HttpOnly
 *       (ativa/expirou/revogada/inexistente → 401 no padrão do projeto);</li>
 *   <li>se o access token expirou, renova no servidor via
 *       {@link GatewayOidcUseCase#refresh(String)} (rotação + lock por sessão);</li>
 *   <li>envia a requisição ao backend com {@code Authorization: Bearer};</li>
 *   <li>em {@code 401} upstream, faz um único refresh e repete (1 tentativa).</li>
 * </ol>
 *
 * <p>Segurança: apenas um allowlist de headers é repassado nas duas direções
 * (nunca {@code Authorization}, {@code Cookie} ou {@code Set-Cookie}), o corpo
 * nunca é logado e nenhum token sai desta classe.
 */
@Component
public class GatewayApiRelay {

    private static final Logger log = LoggerFactory.getLogger(GatewayApiRelay.class);

    /** Headers de request repassados ao backend (whitelist — sem credenciais/hop-by-hop). */
    public static final Set<String> FORWARD_REQUEST_HEADERS = Set.of(
            "Content-Type",
            "Accept",
            "Accept-Language",
            "Range",
            "If-Match",
            "If-None-Match",
            "If-Modified-Since",
            "If-Unmodified-Since");

    /** Headers de resposta devolvidos ao browser (whitelist). */
    private static final Set<String> FORWARD_RESPONSE_HEADERS = Set.of(
            "Content-Type",
            "Content-Disposition",
            "Content-Language",
            "Cache-Control",
            "Expires",
            "ETag",
            "Last-Modified",
            "Location",
            "Retry-After");

    private final OidcGatewayProperties properties;
    private final GatewaySessionResolver sessionResolver;
    private final GatewayOidcUseCase gatewayOidcUseCase;
    private final RestClient restClient;

    public GatewayApiRelay(OidcGatewayProperties properties,
                           GatewaySessionResolver sessionResolver,
                           GatewayOidcUseCase gatewayOidcUseCase) {
        this.properties = properties;
        this.sessionResolver = sessionResolver;
        this.gatewayOidcUseCase = gatewayOidcUseCase;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getApiConnectTimeout());
        requestFactory.setReadTimeout(properties.getApiReadTimeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public ResponseEntity<byte[]> forward(HttpMethod method, String path, String query,
                                          String sessionToken, Map<String, String> requestHeaders,
                                          byte[] body) {
        return forward(method, path, query, sessionToken, requestHeaders, body, 0);
    }

    private ResponseEntity<byte[]> forward(HttpMethod method, String path, String query,
                                           String sessionToken, Map<String, String> requestHeaders,
                                           byte[] body, int attempt) {
        if (!StringUtils.hasText(properties.getApiBackendUrl())) {
            throw new OidcGatewayException("RELAY_NOT_CONFIGURED", 500,
                    "O BFF relay não está configurado neste ambiente.");
        }

        SessionLookup lookup = sessionResolver.resolve(sessionToken);
        if (lookup.status() != SessionStatus.ACTIVE) {
            throw sessionError(lookup);
        }
        GatewaySession session = lookup.session();

        if (session.accessTokenExpiresAt() == null || !session.accessTokenExpiresAt().isAfter(Instant.now())) {
            gatewayOidcUseCase.refresh(sessionToken);
            SessionLookup refreshed = sessionResolver.resolve(sessionToken);
            if (refreshed.status() != SessionStatus.ACTIVE) {
                throw sessionError(refreshed);
            }
            session = refreshed.session();
        }

        ResponseEntity<byte[]> response;
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(buildUri(path, query));
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                spec.header(header.getKey(), header.getValue());
            }
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken());
            if (body != null && body.length > 0) {
                spec.body(body);
            }
            response = spec.exchange((request, raw) -> ResponseEntity
                    .status(raw.getStatusCode())
                    .headers(forwardedResponseHeaders(raw.getHeaders()))
                    .body(raw.getBody().readAllBytes()));
        } catch (RestClientException e) {
            log.warn("API relay upstream failure: user={} method={} path={}",
                    session.userId(), method, path);
            throw new OidcGatewayException("UPSTREAM_UNAVAILABLE", 502,
                    "Serviço temporariamente indisponível.");
        }

        if (response.getStatusCode().value() == 401 && attempt == 0) {
            log.info("API relay retry after refresh: user={}", session.userId());
            gatewayOidcUseCase.refresh(sessionToken);
            return forward(method, path, query, sessionToken, requestHeaders, body, attempt + 1);
        }

        log.info("API relay: user={} method={} path={} status={}",
                session.userId(), method, path, response.getStatusCode().value());
        return response;
    }

    private URI buildUri(String path, String query) {
        String base = properties.getApiBackendUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String normalizedPath = path == null || path.isBlank() ? "" : path;
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        String url = base + normalizedPath;
        if (StringUtils.hasText(query)) {
            url += "?" + query;
        }
        return URI.create(url);
    }

    private HttpHeaders forwardedResponseHeaders(HttpHeaders source) {
        HttpHeaders target = new HttpHeaders();
        for (String name : FORWARD_RESPONSE_HEADERS) {
            List<String> values = source.get(name);
            if (values != null && !values.isEmpty()) {
                target.put(name, new ArrayList<>(values));
            }
        }
        return target;
    }

    private OidcGatewayException sessionError(SessionLookup lookup) {
        return switch (lookup.status()) {
            case EXPIRED -> new OidcGatewayException("SESSION_EXPIRED", 401, "Sessão de gateway expirada.");
            case REVOKED -> new OidcGatewayException("SESSION_REVOKED", 401, "Sessão de gateway revogada.");
            default -> new OidcGatewayException("SESSION_NOT_FOUND", 401, "Sessão de gateway não encontrada.");
        };
    }
}
