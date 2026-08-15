package com.becommerce.crm.application.workflow.dto;

import java.util.Map;

/**
 * Requisição de dry-run (Sprint 15): evento a simular + contexto com os valores
 * das condições. Nenhuma ação real é executada.
 */
public record DryRunRequest(
        String eventType,
        Map<String, Object> context
) {
}
