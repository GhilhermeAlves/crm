package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Valida e normaliza o alvo de redirect pós-login ({@code redirect} em
 * {@code /auth/authorize}) contra a allowlist, prevenindo open redirect
 * (incluindo o clássico {@code //evil.example}).
 *
 * <p>Regras:
 * <ul>
 *   <li>vazio → {@code defaultRedirect};</li>
 *   <li>relativo de mesmo origin (começa com {@code /} e não {@code //}) → aceito;</li>
 *   <li>absoluto → deve usar https (ou http apenas para localhost) e estar na
 *       allowlist (origin ou prefixo de path com {@code *});</li>
 *   <li>qualquer outra forma → rejeitado ({@code OPEN_REDIRECT}).</li>
 * </ul>
 */
@Component
public class RedirectUriValidator {

    private final OidcGatewayProperties properties;

    public RedirectUriValidator(OidcGatewayProperties properties) {
        this.properties = properties;
    }

    public String validateAndNormalize(String redirect) {
        String target = StringUtils.hasText(redirect) ? redirect.trim() : properties.getDefaultRedirect();

        if (target.startsWith("//")) {
            throw openRedirect();
        }

        URI uri = parse(target);
        if (!uri.isAbsolute()) {
            if (target.startsWith("/") && !target.startsWith("//")) {
                return target;
            }
            throw openRedirect();
        }

        if (!isAllowedOrigin(uri)) {
            throw openRedirect();
        }

        return uri.toString();
    }

    private boolean isAllowedOrigin(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        boolean secure = "https".equals(scheme)
                || ("http".equals(scheme) && isLocalhost(uri.getHost()));
        if (!secure) {
            return false;
        }

        String origin = origin(uri);
        for (String entry : properties.getAllowedRedirectUris()) {
            if (!StringUtils.hasText(entry)) {
                continue;
            }
            URI allowed = parse(entry);
            if (!origin(allowed).equals(origin)) {
                continue;
            }
            String entryPath = normalizedPath(allowed);
            String targetPath = normalizedPath(uri);
            if (entryPath.endsWith("*")) {
                if (targetPath.startsWith(entryPath.substring(0, entryPath.length() - 1))) {
                    return true;
                }
            } else if (entryPath.equals("/") || targetPath.startsWith(entryPath)) {
                return true;
            }
        }
        return false;
    }

    private String origin(URI uri) {
        int port = uri.getPort();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + host + (port != -1 ? ":" + port : "");
    }

    private String normalizedPath(URI uri) {
        String path = uri.getPath();
        return (path == null || path.isBlank()) ? "/" : path;
    }

    private boolean isLocalhost(String host) {
        return host != null
                && (host.equalsIgnoreCase("localhost")
                || host.equalsIgnoreCase("127.0.0.1")
                || host.equalsIgnoreCase("[::1]"));
    }

    private URI parse(String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw openRedirect();
        }
    }

    private OidcGatewayException openRedirect() {
        return new OidcGatewayException("OPEN_REDIRECT", 400, "Redirect não permitido pela allowlist.");
    }
}
