package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.workflow.dto.ConditionEvaluation;
import com.becommerce.crm.domain.workflow.ConditionOperator;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowCondition;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Avaliador de condições (Item 3). Compara os campos do contexto do evento
 * contra os valores configurados, aplicando o operador. Suporta apenas os
 * operadores da primeira versão e não avalia código arbitrário: o conjunto de
 * campos é fechado ({@code opportunity.stage/value}, {@code task.priority},
 * {@code activity.type}). Campo ausente no contexto ⇒ condição falsa.
 */
@Component
public class WorkflowConditionEvaluator {

    public boolean matches(Workflow workflow, WorkflowTriggerEvent event) {
        return evaluate(workflow, event.context()).stream().allMatch(ConditionEvaluation::matched);
    }

    /** Compara um único valor do contexto contra o operador/esperado (dry-run). */
    public boolean matches(String field, ConditionOperator operator, String expected, Object actual) {
        return compare(actual, operator, expected);
    }

    /**
     * Avalia cada condição individualmente (Sprint 15), retornando o resultado
     * com o valor esperado e o valor encontrado — usado na persistência de
     * {@code workflow_runs} e no dry-run.
     */
    public List<ConditionEvaluation> evaluate(Workflow workflow, Map<String, Object> context) {
        List<ConditionEvaluation> results = new ArrayList<>();
        for (WorkflowCondition condition : workflow.getConditions()) {
            Object actual = context.get(condition.getField());
            boolean matched = compare(actual, condition.getOperator(), condition.getValue());
            results.add(new ConditionEvaluation(condition.getField(), condition.getOperator(),
                    condition.getValue(), actual, matched));
        }
        return results;
    }

    /** Avalia usando o contexto do próprio evento (conveniência). */
    public List<ConditionEvaluation> evaluate(Workflow workflow, WorkflowTriggerEvent event) {
        return evaluate(workflow, event.context());
    }

    private boolean compare(Object actual, ConditionOperator op, String expected) {
        // Operadores de nulidade não dependem do valor esperado (Sprint 18)
        if (op == ConditionOperator.IS_NULL) {
            return actual == null;
        }
        if (op == ConditionOperator.IS_NOT_NULL) {
            return actual != null;
        }
        if (actual == null) {
            return false;
        }
        if (op == ConditionOperator.CONTAINS) {
            if (expected == null) {
                return false;
            }
            return actual.toString().toLowerCase().contains(expected.trim().toLowerCase());
        }
        if (actual instanceof BigDecimal bd) {
            return numericCompare(bd, op, expected);
        }
        if (actual instanceof Number n) {
            return numericCompare(new BigDecimal(n.toString()), op, expected);
        }
        String s = actual.toString();
        return switch (op) {
            case EQUALS -> s.equalsIgnoreCase(expected.trim());
            case NOT_EQUALS -> !s.equalsIgnoreCase(expected.trim());
            case CONTAINS, IS_NULL, IS_NOT_NULL -> false;
            default -> {
                try {
                    yield numericCompare(new BigDecimal(s), op, expected);
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
        };
    }

    private boolean numericCompare(BigDecimal actual, ConditionOperator op, String expected) {
        BigDecimal exp;
        try {
            exp = new BigDecimal(expected.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        int cmp = actual.compareTo(exp);
        return switch (op) {
            case EQUALS -> cmp == 0;
            case NOT_EQUALS -> cmp != 0;
            case GREATER_THAN -> cmp > 0;
            case LESS_THAN -> cmp < 0;
            case GREATER_OR_EQUAL -> cmp >= 0;
            case LESS_OR_EQUAL -> cmp <= 0;
            case CONTAINS, IS_NULL, IS_NOT_NULL -> false;
        };
    }
}