package com.becommerce.auth.infrastructure.gateway;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Resolve a origem (scheme + host) pela qual o browser enxerga o gateway,
 * independente da cadeia de proxies (nginx → container).
 *
 * <p>Precedência:
 * <ul>
 *   <li>{@code X-Forwarded-Host}: primeiro valor (dev proxy do Next.js local);</li>
 *   <li>senão, {@code Host}/serverName (o nginx repassa {@code Host $host});</li>
 *   <li>scheme {@code https} por padrão — exceto quando o host é localhost, que
 *       é sempre {@code http} (o dev proxy não precisa expor TLS). Isso evita
 *       conflito com o {@code X-Forwarded-Proto} que o nginx sobrescreve.</li>
 * </ul>
 *
 * <p>O resultado NÃO é confiável por si só: o chamador o valida contra a
 * allowlist ({@link RedirectUriValidator}) antes de usar, e qualquer origem não
 * permitida cai no valor fixo configurado.
 */
@Component
public class ForwardedOriginResolver {

    /**
     * @return origem no formato {@code scheme://host[:port]}, ou {@code null}
     *         quando não há como determinar um host confiável.
     */
    public String resolve(HttpServletRequest request) {
        boolean hasForwardedHost = StringUtils.hasText(firstHeader(request, "X-Forwarded-Host"));
        String host = forwardedHost(request);
        if (!StringUtils.hasText(host)) {
            return null;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        boolean localhost = isLocalhostHost(normalized);
        String scheme = localhost
                ? "http"
                : firstHeader(request, "X-Forwarded-Proto");
        scheme = StringUtils.hasText(scheme) ? scheme.trim().toLowerCase(Locale.ROOT) : "https";

        String origin = scheme + "://" + normalized;
        if (!hasForwardedHost && !hasPort(normalized)) {
            origin += withPort(request.getServerPort(), scheme);
        }
        return origin;
    }

    /**
     * O {@code getServerName()} (Host repassado pelo nginx) não inclui a porta;
     * apenas a adiciona quando não é a porta padrão do scheme. Menções de porta
     * já presentes em {@code X-Forwarded-Host} (ex.: {@code localhost:3000})
     * nunca são duplicadas.
     */
    private String withPort(int port, String scheme) {
        int oneOf = 0;
        if ("http".equals(scheme)) {
            oneOf = 80;
        } else if ("https".equals(scheme)) {
            oneOf = 443;
        }
        if (port != -1 && port != oneOf) {
            return ":" + port;
        }
        return "";
    }

    private boolean hasPort(String host) {
        int bracket = host.lastIndexOf(']');
        int colon = host.indexOf(':');
        return colon != -1 && colon > bracket;
    }

    private String forwardedHost(HttpServletRequest request) {
        String forwarded = firstHeader(request, "X-Forwarded-Host");
        if (StringUtils.hasText(forwarded)) {
            return forwarded;
        }
        String serverName = request.getServerName();
        return StringUtils.hasText(serverName) ? serverName : null;
    }

    private String firstHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String first = value.split(",", 2)[0];
        return StringUtils.hasText(first) ? first.trim() : null;
    }

    private boolean isLocalhostHost(String host) {
        String hostOnly = host;
        int colon = host.indexOf(':');
        if (colon != -1) {
            hostOnly = host.substring(0, colon);
        }
        String h = hostOnly.replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1");
    }
}