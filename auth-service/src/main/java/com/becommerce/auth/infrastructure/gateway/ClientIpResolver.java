package com.becommerce.auth.infrastructure.gateway;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Resolve o endereço IP real do cliente respeitando a cadeia de proxies real da
 * aplicação (Sprint 6.7 — seção de segurança).
 *
 * <p>O nginx da VPS configura:
 * <ul>
 *   <li>{@code proxy_set_header X-Real-IP $remote_addr} — <b>sobrescreve</b> o
 *       header, portanto nunca é forjável pelo cliente;</li>
 *   <li>{@code proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for} —
 *       anexa {@code $remote_addr} <b>ao final</b> da lista; os valores iniciais
 *       podem ter sido fornecidos pelo cliente.</li>
 * </ul>
 *
 * <p>Por isso a prioridade de confiança é:
 * <ol>
 *   <li>{@code X-Real-IP} (sobrescrito pelo proxy confiável);</li>
 *   <li>o <b>último</b> valor plausível de {@code X-Forwarded-For} (o valor real
 *       anexado pelo proxy);</li>
 *   <li>{@code remoteAddr} (conexão direta).</li>
 * </ol>
 *
 * <p>Não aceita cegamente o primeiro valor de {@code X-Forwarded-For}: com
 * {@code $proxy_add_x_forwarded_for} o primeiro valor é controlado pelo cliente e
 * permitiria spoofing de IP no rate limiting.
 */
@Component
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:]+");

    public String resolve(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (isPlausibleIp(realIp)) {
            return realIp;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String[] parts = forwardedFor.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (isPlausibleIp(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }

    public boolean isPlausibleIp(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return IPV4.matcher(value).matches() || IPV6.matcher(value).matches();
    }
}
