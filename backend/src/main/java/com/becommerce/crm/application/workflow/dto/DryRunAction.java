package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.ActionType;

import java.util.Map;

/**
 * Ação que SERIA executada no dry-run (Sprint 15). Nada é de fato executado:
 * apenas a intenção é exibida (tipo + parâmetros relevantes).
 */
public record DryRunAction(
        ActionType actionType,
        int sortOrder,
        Map<String, Object> params
) {
}
