package com.becommerce.crm.application.ai.port.output;

import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.domain.ai.AgentActionAudit;

import java.util.UUID;

/**
 * Trilha append-only das ações do agente (tabela {@code agent_action_audit}, V077):
 * só inserção e leitura, sem atualização ou exclusão.
 */
public interface AgentActionAuditRepository {

    AgentActionAudit save(AgentActionAudit audit);

    PageResponse<AgentActionAudit> findByConversationId(UUID conversationId, int page, int size);

    PageResponse<AgentActionAudit> findByCompanyId(UUID companyId, int page, int size);
}
