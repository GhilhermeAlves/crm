package com.becommerce.crm.domain.ai;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AiActionTest {

    @Test
    void shouldCreateProposedActionWithNullVersion() {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        AiAction action = AiAction.propose(companyId, userId, conversationId, "create_activity",
                "ACTIVITY", null, Map.of("type", "CALL", "subject", "Follow-up"), "Registrar atividade");

        assertNotNull(action.getId());
        assertEquals(companyId, action.getCompanyId());
        assertEquals(userId, action.getUserId());
        assertEquals(conversationId, action.getConversationId());
        assertEquals("create_activity", action.getTool());
        assertEquals(AiActionStatus.PROPOSED, action.getStatus());
        assertNull(action.getResult());
        assertNull(action.getErrorMessage());
        // Nova entidade deve ter version nulo para ser tratada como novo INSERT
        // (version != null faria o Spring Data isNew()==false -> merge -> UPDATE
        // em linha inexistente -> optimistic-lock error).
        assertNull(action.getVersion());
    }
}