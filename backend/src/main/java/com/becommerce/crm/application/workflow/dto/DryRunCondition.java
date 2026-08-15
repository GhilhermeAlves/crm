package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.ConditionOperator;

/**
 * Condição avaliada durante o dry-run (Sprint 15). Mostra o que foi avaliado,
 * o valor esperado, o valor informado pelo usuário e o resultado.
 */
public record DryRunCondition(
        String field,
        ConditionOperator operator,
        String expected,
        Object actual,
        boolean matched
) {
}
