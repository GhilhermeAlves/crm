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
        String host = forwardedHost(request);
        if (!StringUtils.hasText(host)) {
            return null;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        String scheme = isLocalhostHost(normalized)
                ? "http"
                : firstHeader(request, "X-Forwarded-Proto");
        if (StringUtils.hasText(scheme)) {
            return scheme.trim().toLowerCase(Locale.ROOT) + "://" + normalized;
        }
        return "https://" + normalized;
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