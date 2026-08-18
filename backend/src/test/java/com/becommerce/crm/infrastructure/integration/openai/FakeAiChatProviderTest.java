package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FakeAiChatProviderTest {

    private final FakeAiChatProvider provider = new FakeAiChatProvider();

    @Test
    void shouldReturnProviderName() {
        assertEquals("FAKE", provider.providerName());
    }

    @Test
    void shouldAnswerBasedOnUserMessage() {
        var request = new AiProvider.ChatRequest(UUID.randomUUID(), UUID.randomUUID(), List.of(
                new AiProvider.ChatMessage("system", "sistema"),
                new AiProvider.ChatMessage("user", "Como está esse cliente?")));
        String answer = provider.chat(request);
        assertNotNull(answer);
        assertTrue(answer.contains("Como está esse cliente?"));
        assertTrue(answer.contains("fake"));
    }

    @Test
    void shouldGreetWhenNoUserMessage() {
        var request = new AiProvider.ChatRequest(UUID.randomUUID(), UUID.randomUUID(), List.of(
                new AiProvider.ChatMessage("system", "sistema")));
        String answer = provider.chat(request);
        assertNotNull(answer);
        assertTrue(answer.contains("Como posso ajudar"));
    }
}