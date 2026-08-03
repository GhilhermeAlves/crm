package com.becommerce.auth.infrastructure.gateway;

import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Fabrica os cookies do Access Gateway (Sprints 6.1/6.2):
 *
 * <ul>
 *   <li>cookie de sessão — HttpOnly, SameSite=Lax, Secure (quando habilitado),
 *       Max-Age = TTL da sessão; valor é sempre o {@code sessionToken} opaco;</li>
 *   <li>cookie CSRF — <b>não</b> HttpOnly (o browser precisa lê-lo para enviar o
 *       header {@code X-XSRF-TOKEN}), mesmo Path/SameSite/Secure/Max-Age;</li>
 *   <li>cookie de sessão expirado — Max-Age=0, para limpar o cookie no logout.</li>
 * </ul>
 */
@Component
public class GatewayCookieFactory {

    private final OidcGatewayProperties properties;

    public GatewayCookieFactory(OidcGatewayProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie createSessionCookie(String sessionToken) {
        return base(properties.getCookieName(), sessionToken)
                .httpOnly(true)
                .maxAge(properties.getSessionTtl())
                .build();
    }

    public ResponseCookie createCsrfCookie(String csrfToken) {
        return base(properties.getCsrfCookieName(), csrfToken)
                .httpOnly(false)
                .maxAge(properties.getSessionTtl())
                .build();
    }

    public ResponseCookie createExpiredSessionCookie() {
        return base(properties.getCookieName(), "")
                .httpOnly(true)
                .maxAge(Duration.ZERO)
                .build();
    }

    public Optional<String> readSessionToken(Cookie[] cookies) {
        return read(cookies, properties.getCookieName());
    }

    public Optional<String> readCsrfToken(Cookie[] cookies) {
        return read(cookies, properties.getCsrfCookieName());
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path("/")
                .sameSite("Lax")
                .maxAge(properties.getSessionTtl());
        if (properties.isSecureCookie()) {
            builder.secure(true);
        }
        return builder;
    }

    private Optional<String> read(Cookie[] cookies, String name) {
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
