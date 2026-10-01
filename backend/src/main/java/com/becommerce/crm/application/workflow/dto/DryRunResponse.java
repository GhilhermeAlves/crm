package com.becommerce.crm.application.workflow.dto;

import java.util.List;

/**
 * Resultado do dry-run de um workflow (Sprint 15): elegibilidade da regra para
 * o evento/contexto informados, resultado de cada condição e as ações que
 * seriam executadas — sem nenhum efeito colateral.
 */
public record DryRunResponse(
        boolean matched,
        String eventType,
        List<DryRunCondition> conditions,
        List<DryRunAction> actions,
        String message
) {
}
