package com.becommerce.crm.application.ai.port.output;

import com.becommerce.crm.domain.ai.AgentConfig;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para a configuração do agente de IA (Sprint 1 da portabilidade
 * Q7 → CRM). Acesso tenant-scoped (RLS FORCE via GUC). A ausência de config é
 * um estado válido: significa que o agente está desabilitado (safe default).
 */
public interface AgentConfigRepository {

    Optional<AgentConfig> findByCompanyId(UUID companyId);

    AgentConfig save(AgentConfig agentConfig);
}