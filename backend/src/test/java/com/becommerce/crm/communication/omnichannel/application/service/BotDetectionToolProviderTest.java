package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BotDetectionToolProviderTest {

    private final AutomatedSenderGuard guard = mock(AutomatedSenderGuard.class);
    private final BotDetectionToolProvider provider = new BotDetectionToolProvider(guard);
    private final AgentToolSession session = new AgentToolSession(UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), null, "5534999990000", "Fulano", UUID.randomUUID());

    @Test
    void agentSuspicionHandsConversationToTeamAndNeverIgnoresTheNumber() {
        String result = provider.execute(session, new AiProvider.ToolCall("1",
                BotDetectionToolProvider.HAND_TO_TEAM, Map.of("motivo", "mensagem pessoal")));

        verify(guard).handToTeam(session.companyId(), session.conversationId(), "5534999990000",
                "agente: mensagem pessoal");
        verify(guard, never()).ignore(any(), any(), any(), any());
        assertTrue(result.contains(BotDetectionToolProvider.SILENCE));
    }

    @Test
    void toolIsNamedForTheTeamHandoff() {
        assertEquals(BotDetectionToolProvider.HAND_TO_TEAM, provider.definitions().get(0).name());
        assertEquals("deixar_com_equipe", provider.definitions().get(0).name());
    }
}
