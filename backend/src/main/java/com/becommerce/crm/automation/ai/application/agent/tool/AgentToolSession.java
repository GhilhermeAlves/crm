package com.becommerce.crm.automation.ai.application.agent.tool;

import java.util.UUID;

/**
 * Escopo de execução das ferramentas numa interação. Os argumentos vindos do
 * modelo NUNCA definem empresa/contato — sempre esta sessão.
 */
public record AgentToolSession(UUID companyId, UUID agentConfigId, UUID conversationId, UUID contactId,
                               String phone, String senderName, UUID inboundMessageId) {
}
