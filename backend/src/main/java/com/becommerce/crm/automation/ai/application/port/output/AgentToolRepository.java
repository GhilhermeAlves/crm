package com.becommerce.crm.automation.ai.application.port.output;

import com.becommerce.crm.automation.ai.domain.AgentTool;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ferramentas que o agente pode invocar, por empresa (tabela {@code agent_tool}, V077).
 * Isolamento por tenant garantido pelo RLS FORCE da tabela.
 */
public interface AgentToolRepository {

    List<AgentTool> findByCompanyId(UUID companyId);

    Optional<AgentTool> findByCompanyAndName(UUID companyId, String toolName);

    Optional<AgentTool> findById(UUID id);

    AgentTool save(AgentTool tool);

    void delete(UUID id);
}
