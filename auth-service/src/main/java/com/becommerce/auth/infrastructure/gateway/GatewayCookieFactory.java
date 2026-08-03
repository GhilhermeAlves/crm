package com.becommerce.auth.infrastructure.gateway;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Fabrica o cookie de sessão do Access Gateway: HttpOnly, SameSite=Lax,
 * Secure (quando habilitado) e Max-Age = TTL da sessão. O valor é sempre o
 * {@code sessionToken} opaco — nunca um JWT ou dados sensíveis.
 */
@Component
public class GatewayCookieFactory {

    private final OidcGatewayProperties properties;

    public GatewayCookieFactory(OidcGatewayProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie createSessionCookie(String sessionToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getCookieName(), sessionToken)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(properties.getSessionTtl());
        if (properties.isSecureCookie()) {
            builder.secure(true);
        }
        return builder.build();
    }
}
