package com.becommerce.auth.infrastructure.observability;

import com.becommerce.auth.infrastructure.gateway.SecureTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationIdFilterTest {

    private static final String LONG = "x".repeat(129);

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter(new SecureTokenGenerator());
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String header = response.getHeader("X-Correlation-Id");
        assertNotNull(header, "resposta deve conter X-Correlation-Id");
        assertTrue(header.length() >= 8 && header.length() <= 128, "ID deve ter tamanho limitado");
        assertTrue(header.matches("[A-Za-z0-9_.\\-:]+"), "ID deve usar charset seguro");
        assertTrue(chain.getRequest() != null, "chain deve prosseguir");
    }

    @Test
    void shouldPreserveValidCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/authorize");
        request.addHeader("X-Correlation-Id", "abc-123_DEF:456.789");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("abc-123_DEF:456.789", response.getHeader("X-Correlation-Id"));
    }

    @Test
    void shouldRejectOversizedCorrelationIdAndGenerateNew() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/authorize");
        request.addHeader("X-Correlation-Id", LONG);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String header = response.getHeader("X-Correlation-Id");
        assertNotNull(header);
        assertNotEquals(LONG, header);
        assertTrue(header.length() <= 128);
    }

    @Test
    void shouldRejectMalformedCorrelationIdAndGenerateNew() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/authorize");
        request.addHeader("X-Correlation-Id", "has spaces and <html>");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String header = response.getHeader("X-Correlation-Id");
        assertNotNull(header);
        assertFalse(header.contains(" "), "ID gerado não pode conter caracteres inválidos");
        assertTrue(header.matches("[A-Za-z0-9_.\\-:]{8,128}"));
    }

    @Test
    void shouldRejectShortCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/authorize");
        request.addHeader("X-Correlation-Id", "abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String header = response.getHeader("X-Correlation-Id");
        assertNotNull(header);
        assertNotEquals("abc", header);
    }

    @Test
    void shouldExposeCorrelationIdInContextAndMdcAndClearAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] mdcDuringRequest = new String[1];
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                mdcDuringRequest[0] = MDC.get("correlationId");
                CorrelationIdContext.set("context-during-request");
            }
        };

        filter.doFilter(request, response, chain);

        String header = response.getHeader("X-Correlation-Id");
        assertEquals(header, mdcDuringRequest[0], "MDC deve conter o correlation ID durante a requisição");
        assertNull(CorrelationIdContext.get(), "contexto deve ser limpo ao final da requisição");
        assertNull(MDC.get("correlationId"), "MDC deve ser limpo ao final da requisição");
    }

    @Test
    void shouldSetResponseHeaderEvenOnErrorPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                throw new RuntimeException("boom");
            }
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException expected) {
            // ignorado: o header deve ter sido definido antes da cadeia
        }

        assertNotNull(response.getHeader("X-Correlation-Id"));
        assertNull(CorrelationIdContext.get(), "contexto deve ser limpo mesmo com exceção");
    }
}
