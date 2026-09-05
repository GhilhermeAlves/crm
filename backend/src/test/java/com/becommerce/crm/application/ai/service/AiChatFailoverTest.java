package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.domain.ai.AiProviderException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Failover da IA autônoma (Sprint 2): primário → fallback com regras de
 * classificação de erro. Cobre exatamente os 5 casos da especificação.
 */
class AiChatFailoverTest {

    private final UUID companyId = UUID.randomUUID();
    private final AiProvider.ChatRequest request = new AiProvider.ChatRequest(
            companyId, UUID.randomUUID(), List.of(new AiProvider.ChatMessage("user", "oi")));

    @Test
    void primaryOk_shouldUsePrimaryAndNotFallback() {
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(primary.providerName()).thenReturn("PRIMARY");
        when(primary.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("ok"));
        when(fallback.providerName()).thenReturn("FALLBACK");

        AiProvider.ChatResult result = new AiChatFailover(List.of(primary, fallback)).chat(request);

        assertEquals("ok", result.content());
        verify(primary).chatWithTools(any());
        verify(fallback, never()).chatWithTools(any());
    }

    @Test
    void primaryTimeout_fallbackOk_shouldUseFallbackWithOneResult() {
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(primary.providerName()).thenReturn("PRIMARY");
        when(primary.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));
        when(fallback.providerName()).thenReturn("FALLBACK");
        when(fallback.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("do fallback"));

        AiProvider.ChatResult result = new AiChatFailover(List.of(primary, fallback)).chat(request);

        assertEquals("do fallback", result.content());
        verify(primary).chatWithTools(any());
        verify(fallback).chatWithTools(any());
    }

    @Test
    void primary429_fallbackOk_shouldUseFallback() {
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(primary.providerName()).thenReturn("PRIMARY");
        when(primary.chatWithTools(any())).thenThrow(new AiProviderException("rate limit", true));
        when(fallback.providerName()).thenReturn("FALLBACK");
        when(fallback.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("recuperado"));

        AiProvider.ChatResult result = new AiChatFailover(List.of(primary, fallback)).chat(request);

        assertEquals("recuperado", result.content());
        verify(fallback).chatWithTools(any());
        assertTrue(fallback.providerName().startsWith("FALLBACK"));
    }

    @Test
    void primaryNonRecoverable_shouldNotRunFallback() {
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(primary.providerName()).thenReturn("PRIMARY");
        when(primary.chatWithTools(any())).thenThrow(new AiProviderException("config inválida", false));
        when(fallback.providerName()).thenReturn("FALLBACK");

        AiChatFailover failover = new AiChatFailover(List.of(primary, fallback));

        AiProviderException ex = assertThrows(AiProviderException.class, () -> failover.chat(request));
        assertTrue(ex.getMessage().contains("config inválida"));
        assertTrue(!ex.isRecoverable());
        verify(fallback, never()).chatWithTools(any());
    }

    @Test
    void bothFail_shouldRethrowAndProduceNoResult() {
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(primary.providerName()).thenReturn("PRIMARY");
        when(primary.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));
        when(fallback.providerName()).thenReturn("FALLBACK");
        when(fallback.chatWithTools(any())).thenThrow(new AiProviderException("provider down", true));

        AiChatFailover failover = new AiChatFailover(List.of(primary, fallback));

        AiProviderException ex = assertThrows(AiProviderException.class, () -> failover.chat(request));
        assertEquals("provider down", ex.getMessage());
        assertTrue(ex.isRecoverable());
    }

    @Test
    void noProviders_shouldFailFastWithoutCallingAnything() {
        AiChatFailover failover = new AiChatFailover(List.of());

        AiProviderException ex = assertThrows(AiProviderException.class, () -> failover.chat(request));
        assertTrue(ex.getMessage().contains("Nenhum provider"));
        assertTrue(!ex.isRecoverable());
    }
}