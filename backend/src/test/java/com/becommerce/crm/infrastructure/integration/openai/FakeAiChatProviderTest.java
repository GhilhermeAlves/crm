package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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

    @Test
    void shouldRequestToolWithValidRequiredArguments() {
        var tool = new AiProvider.ToolDefinition("create_activity", "Propoe uma atividade", Map.of(
                "type", "object",
                "properties", Map.of(
                        "type", Map.of("type", "string", "enum", List.of("CALL", "MEETING")),
                        "subject", Map.of("type", "string")),
                "required", List.of("type", "subject")));
        var request = new AiProvider.ChatRequest(UUID.randomUUID(), UUID.randomUUID(),
                List.of(new AiProvider.ChatMessage("user", "Liste meus clientes.")),
                List.of(tool));
        var result = provider.chatWithTools(request);
        assertTrue(result.hasToolCalls());
        assertEquals("create_activity", result.toolCalls().get(0).name());
        assertEquals("CALL", result.toolCalls().get(0).arguments().get("type"));
        assertNotNull(result.toolCalls().get(0).arguments().get("subject"));
    }

    @Test
    void shouldProduceFinalAnswerFromToolResult() {
        var request = new AiProvider.ChatRequest(UUID.randomUUID(), UUID.randomUUID(),
                List.of(
                        new AiProvider.ChatMessage("user", "Liste meus clientes."),
                        new AiProvider.ChatMessage("tool", "{\"success\":true,\"data\":null}")),
                List.of());
        var result = provider.chatWithTools(request);
        assertNotNull(result.content());
        assertFalse(result.hasToolCalls());
        assertTrue(result.content().contains("Liste meus clientes."));
    }
}