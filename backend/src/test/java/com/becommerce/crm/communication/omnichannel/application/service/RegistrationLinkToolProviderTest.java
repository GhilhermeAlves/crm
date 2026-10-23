package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationLinkResponse;
import com.becommerce.crm.masterdata.contact.registration.application.service.RegistrationLinkService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationLinkToolProviderTest {

    private final RegistrationLinkService service = mock(RegistrationLinkService.class);
    private final RegistrationLinkToolProvider provider = new RegistrationLinkToolProvider(service);
    private final AgentConfig agent = mock(AgentConfig.class);
    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    private AgentToolSession session(UUID contact) {
        return new AgentToolSession(companyId, UUID.randomUUID(), UUID.randomUUID(), contact,
                "5511999999999", "Maria", UUID.randomUUID());
    }

    private static AiProvider.ToolCall call() {
        return new AiProvider.ToolCall("c1", RegistrationLinkToolProvider.SEND_LINK, Map.of());
    }

    @Test
    void disponivelSomenteComContato() {
        assertTrue(provider.isAvailable(agent, session(contactId)));
        assertFalse(provider.isAvailable(agent, session(null)));
    }

    @Test
    void expoeFerramentaEGuidance() {
        assertTrue(provider.handles("enviar_link_cadastro"));
        assertTrue(provider.guidance(agent, session(contactId)).orElseThrow().contains("não peça CPF"));
    }

    @Test
    void geraLinkComEscopoDaSessaoESemAtor() {
        when(service.create(companyId, contactId, null))
                .thenReturn(new RegistrationLinkResponse("https://app/cadastro/abc", LocalDateTime.now()));

        String result = provider.execute(session(contactId), call());

        assertEquals("Link de cadastro: https://app/cadastro/abc (válido por 7 dias). Envie ao paciente.", result);
        verify(service).create(companyId, contactId, null);
    }

    @Test
    void falhaDevolveMensagemSegura() {
        when(service.create(any(), any(), isNull())).thenThrow(new IllegalStateException("boom"));

        String result = provider.execute(session(contactId), call());

        assertTrue(result.startsWith("Não foi possível gerar o link"));
    }
}
