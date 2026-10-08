package com.becommerce.crm.automation.ai.application.agent.context;

import com.becommerce.crm.automation.ai.application.agent.tool.ToolContext;
import com.becommerce.crm.automation.ai.domain.AgentConfig;

/**
 * Contexto completo de UMA execução do agente, com cada responsabilidade
 * separada. Só o {@code AgentContextRenderer} converte isto em mensagens do
 * provider — nenhum outro componente monta pedaços de prompt.
 */
public record AgentContext(
        AgentConfig agent,
        ClinicContext clinic,
        PatientContext patient,
        ConversationHistory history,
        MemoryContext memory,
        KnowledgeContext knowledge,
        ToolContext tools,
        String currentMessage
) {
}
