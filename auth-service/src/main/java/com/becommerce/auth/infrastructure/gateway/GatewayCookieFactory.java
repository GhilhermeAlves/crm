package com.becommerce.auth.infrastructure.gateway;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ForwardedOriginResolver forwardedOriginResolver;

    public GatewayCookieFactory(OidcGatewayProperties properties, ForwardedOriginResolver forwardedOriginResolver) {
        this.properties = properties;
        this.forwardedOriginResolver = forwardedOriginResolver;
    }

    public ResponseCookie createSessionCookie(String sessionToken, HttpServletRequest request) {
        return base(properties.getCookieName(), sessionToken, request)
                .httpOnly(true)
                .maxAge(properties.getSessionTtl())
                .build();
    }

    public ResponseCookie createCsrfCookie(String csrfToken, HttpServletRequest request) {
        return base(properties.getCsrfCookieName(), csrfToken, request)
                .httpOnly(false)
                .maxAge(properties.getSessionTtl())
                .build();
    }

    public ResponseCookie createExpiredSessionCookie(HttpServletRequest request) {
        return base(properties.getCookieName(), "", request)
                .httpOnly(true)
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie createExpiredCsrfCookie(HttpServletRequest request) {
        return base(properties.getCsrfCookieName(), "", request)
                .httpOnly(false)
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie createPendingLinkCookie(String pendingToken, HttpServletRequest request) {
        return base(properties.getPendingLinkCookieName(), pendingToken, request)
                .httpOnly(true)
                .maxAge(properties.getPendingLinkTtl())
                .build();
    }

    public ResponseCookie createExpiredPendingLinkCookie(HttpServletRequest request) {
        return base(properties.getPendingLinkCookieName(), "", request)
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

    public Optional<String> readPendingLinkToken(Cookie[] cookies) {
        return read(cookies, properties.getPendingLinkCookieName());
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value, HttpServletRequest request) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path("/")
                .sameSite("Lax")
                .maxAge(properties.getSessionTtl());
        if (properties.isSecureCookie() && !isLocalDevOrigin(request)) {
            builder.secure(true);
        }
        return builder;
    }

    /**
     * Dev local (ex.: {@code http://localhost:3000} via proxy/middleware do
     * Next.js): o browser rejeita cookie com {@code Secure} sobre http, então
     * o flag é omitido apenas quando a origem deriva de localhost. Produção
     * (https via nginx) e qualquer origem sem host confiável mantêm o flag
     * conforme configurado ({@link OidcGatewayProperties#isSecureCookie()}).
     */
    private boolean isLocalDevOrigin(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String origin = forwardedOriginResolver.resolve(request);
        return origin != null
                && (origin.startsWith("http://localhost")
                || origin.startsWith("http://127.0.0.1")
                || origin.startsWith("http://[::1]"));
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
