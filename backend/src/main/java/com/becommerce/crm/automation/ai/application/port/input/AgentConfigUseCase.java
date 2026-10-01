package com.becommerce.crm.automation.ai.application.port.input;

import com.becommerce.crm.automation.ai.application.dto.AgentConfigRequest;
import com.becommerce.crm.automation.ai.application.dto.AgentConfigResponse;

import java.util.UUID;

/**
 * Admin da configuração do agente de IA (Sprint 3 - AgentConfig Administrativo).
 * A empresa é SEMPRE derivada do usuário autenticado ({@code CurrentUser.companyId})
 * — nunca do request.
 */
public interface AgentConfigUseCase {

    AgentConfigResponse get(UUID companyId);

    AgentConfigResponse update(UUID companyId, AgentConfigRequest request);
}