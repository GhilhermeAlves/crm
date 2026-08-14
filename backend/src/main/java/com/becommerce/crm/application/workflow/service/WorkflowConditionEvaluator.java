package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.domain.workflow.ConditionOperator;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowCondition;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
        if (workflow.getConditions().isEmpty()) {
            return true;
        }
        Map<String, Object> context = event.context();
        for (WorkflowCondition condition : workflow.getConditions()) {
            Object actual = context.get(condition.getField());
            if (!compare(actual, condition.getOperator(), condition.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean compare(Object actual, ConditionOperator op, String expected) {
        if (actual == null) {
            return false;
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
        };
    }
}