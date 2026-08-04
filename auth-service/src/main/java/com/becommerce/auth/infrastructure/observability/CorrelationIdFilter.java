package com.becommerce.auth.infrastructure.observability;

import com.becommerce.auth.infrastructure.gateway.SecureTokenGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Filtro de {@code Correlation ID} (Sprint 6.6). Roda antes de toda a cadeia
 * (incluindo o Spring Security) e garante que toda requisição possua um
 * identificador opaco, não previsível e limitado em tamanho.
 *
 * <ul>
 *   <li>Se o cliente enviar {@code X-Correlation-Id} com formato/tamanho válidos,
 *       o valor é preservado;</li>
 *   <li>Ausente ou inválido → gera um novo valor criptograficamente aleatório;</li>
 *   <li>O valor é publicado no header de resposta {@code X-Correlation-Id}, no
 *       contexto da requisição ({@link CorrelationIdContext}) e no MDC
 *       ({@code correlationId}) para o logging;</li>
 *   <li>Nunca são usados como correlation id tokens/cookies/sessionId/secrets —
 *       o valor aceito é restrito a um charset seguro e nunca é interpretado.</li>
 * </ul>
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Za-z0-9_.\\-:]{8,128}$");
    private static final int MAX_LENGTH = 128;

    private final SecureTokenGenerator tokenGenerator;

    public CorrelationIdFilter(SecureTokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolve(request);
        CorrelationIdContext.set(correlationId);
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIdContext.clear();
            MDC.remove(MDC_KEY);
        }
    }

    private String resolve(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER);
        if (incoming != null
                && incoming.length() <= MAX_LENGTH
                && VALID_PATTERN.matcher(incoming).matches()) {
            return incoming;
        }
        return tokenGenerator.urlSafe(16);
    }
}
