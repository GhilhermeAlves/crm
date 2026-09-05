package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.domain.ai.AiProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Classificação de erros do provider de IA (Sprint 2): timeout/429/5xx e falhas
 * de rede são RECUPERÁVEIS (candidatas a fallback); 4xx de config/request são
 * NÃO recuperáveis. Também cobre o caso em que o {@code block()} do WebClient
 * re-embrulha o {@link TimeoutException} na cadeia de causas.
 */
class OpenAiChatProviderErrorClassificationTest {

    @Test
    void http429_shouldBeRecoverable() {
        assertTrue(OpenAiChatProvider.isRecoverable(response(429)));
    }

    @Test
    void http408_shouldBeRecoverable() {
        assertTrue(OpenAiChatProvider.isRecoverable(response(408)));
    }

    @Test
    void http425_shouldBeRecoverable() {
        assertTrue(OpenAiChatProvider.isRecoverable(response(425)));
    }

    @Test
    void http5xx_shouldBeRecoverable() {
        assertTrue(OpenAiChatProvider.isRecoverable(response(500)));
        assertTrue(OpenAiChatProvider.isRecoverable(response(502)));
        assertTrue(OpenAiChatProvider.isRecoverable(response(503)));
    }

    @Test
    void other4xx_shouldNotBeRecoverable() {
        assertFalse(OpenAiChatProvider.isRecoverable(response(400)));
        assertFalse(OpenAiChatProvider.isRecoverable(response(401)));
        assertFalse(OpenAiChatProvider.isRecoverable(response(403)));
        assertFalse(OpenAiChatProvider.isRecoverable(response(404)));
    }

    @Test
    void transportFailure_shouldBeRecoverable() {
        WebClientRequestException ex = new WebClientRequestException(new IOException("boom"),
                HttpMethod.POST, URI.create("http://localhost/v1/chat/completions"), new HttpHeaders());
        assertTrue(OpenAiChatProvider.isRecoverable(ex));
    }

    @Test
    void timeoutException_shouldBeRecoverable() {
        assertTrue(OpenAiChatProvider.isRecoverable(new TimeoutException("timeout")));
        assertTrue(OpenAiChatProvider.isRecoverable(new SocketTimeoutException("timeout")));
    }

    @Test
    void wrappedTimeout_shouldBeRecoverableThroughCauseChain() {
        // Mono.block() re-embrulha a TimeoutException checada em RuntimeException;
        // a classificação deve olhar a cadeia de causas.
        RuntimeException wrapped = new RuntimeException(new TimeoutException("timeout"));
        assertTrue(OpenAiChatProvider.isRecoverable(wrapped));
    }

    @Test
    void unknownRuntimeException_shouldNotBeRecoverable() {
        assertFalse(OpenAiChatProvider.isRecoverable(new IllegalStateException("boom")));
    }

    @Test
    void providerException_shouldCarryRecoverableFlag() {
        AiProviderException recoverable = new AiProviderException("timeout", true);
        AiProviderException notRecoverable = new AiProviderException("config inválida", false);
        assertTrue(recoverable.isRecoverable());
        assertFalse(notRecoverable.isRecoverable());
        assertFalse(new AiProviderException("default conservador").isRecoverable());
        assertThrows(AiProviderException.class, () -> { throw recoverable; });
    }

    private static WebClientResponseException response(int status) {
        return WebClientResponseException.create(status,
                "Status " + status, new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    }
}