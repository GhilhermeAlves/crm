package com.becommerce.crm.automation.ai.application.agent.context;

import java.util.UUID;

/**
 * Dados da interação que disparou o agente (canal → runtime). O escopo de
 * empresa vem sempre do evento autenticado/RLS, nunca do conteúdo da mensagem.
 */
public record AgentRuntimeInput(
        UUID companyId,
        UUID conversationId,
        UUID contactId,
        String phone,
        String senderName,
        UUID inboundMessageId,
        String currentMessage,
        ConversationHistory history
) {
}
