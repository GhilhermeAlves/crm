package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validação semântica dos tokens retornados no callback (Sprint 6.1). A
 * assinatura, expiração e issuer são validados pelo {@link JwtDecoder} (JWKS do
 * Keycloak); aqui são checados os claims de segurança OIDC:
 *
 * <ul>
 *   <li>ID token: {@code nonce} (anti-replay), {@code aud} contém o clientId,
 *       {@code azp} == clientId quando presente;</li>
 *   <li>access token: {@code aud} contém o clientId ou uma audience configurada;</li>
 *   <li>ambos: {@code exp} e {@code iat} com tolerância de clock skew.</li>
 * </ul>
 */
@Component
public class OidcTokenValidator {

    private final JwtDecoder jwtDecoder;
    private final OidcGatewayProperties properties;

    public OidcTokenValidator(JwtDecoder jwtDecoder, OidcGatewayProperties properties) {
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
    }

    public Jwt validateIdToken(String idTokenValue, String expectedNonce) {
        Jwt jwt = decode(idTokenValue);

        requireIssuer(jwt);
        requireAudience(jwt, Set.of(properties.getClientId()));
        String azp = jwt.getClaimAsString("azp");
        if (azp != null && !azp.isBlank() && !properties.getClientId().equals(azp)) {
            throw invalid("azp");
        }
        requireFresh(jwt);

        String nonce = jwt.getClaimAsString("nonce");
        if (!Objects.equals(expectedNonce, nonce)) {
            throw invalid("nonce");
        }
        return jwt;
    }

    public Jwt validateAccessToken(String accessTokenValue) {
        Jwt jwt = decode(accessTokenValue);

        requireIssuer(jwt);
        Set<String> audiences = new HashSet<>();
        audiences.add(properties.getClientId());
        audiences.addAll(properties.getTokenAudiences());
        requireAudience(jwt, audiences);
        requireFresh(jwt);
        return jwt;
    }

    private Jwt decode(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw invalid("ausência de token");
        }
        try {
            return jwtDecoder.decode(tokenValue);
        } catch (JwtException | IllegalArgumentException e) {
            throw new OidcGatewayException("TOKEN_VALIDATION_FAILED", 401,
                    "Token inválido ou assinatura não confiável.");
        }
    }

    private void requireIssuer(Jwt jwt) {
        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        if (!properties.getIssuerUri().equals(issuer)) {
            throw invalid("issuer");
        }
    }

    private void requireAudience(Jwt jwt, Set<String> allowed) {
        List<String> audience = jwt.getAudience() == null ? List.of() : new ArrayList<>(jwt.getAudience());
        audience.retainAll(allowed);
        if (audience.isEmpty()) {
            throw invalid("aud");
        }
    }

    private void requireFresh(Jwt jwt) {
        Instant now = Instant.now();
        if (jwt.getExpiresAt() == null || jwt.getExpiresAt().isBefore(now.minus(properties.getClockSkew()))) {
            throw invalid("exp");
        }
        if (jwt.getIssuedAt() != null && jwt.getIssuedAt().isAfter(now.plus(properties.getClockSkew()))) {
            throw invalid("iat");
        }
    }

    private OidcGatewayException invalid(String claim) {
        return new OidcGatewayException("TOKEN_VALIDATION_FAILED", 401,
                "Token rejeitado: claim " + claim + " inválido.");
    }
}
