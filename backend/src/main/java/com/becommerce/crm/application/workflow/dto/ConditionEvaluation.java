package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.ConditionOperator;

/**
 * Resultado da avaliação de uma condição (Sprint 15). Usado na persistência de
 * {@code workflow_runs} e no dry-run: mostra o que foi avaliado (campo/operador),
 * o valor esperado, o valor encontrado no contexto e o resultado.
 */
public record ConditionEvaluation(
        String field,
        ConditionOperator operator,
        String expected,
        Object actual,
        boolean matched
) {
}
