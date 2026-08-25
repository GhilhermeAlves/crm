package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.domain.workflow.ConditionOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Operadores novos da Sprint 18: CONTAINS, IS_NULL, IS_NOT_NULL. */
class WorkflowConditionEvaluatorSprint18Test {

    private final WorkflowConditionEvaluator evaluator = new WorkflowConditionEvaluator();

    @Test
    void containsIsCaseInsensitive() {
        assertTrue(evaluator.matches("lead.status", ConditionOperator.CONTAINS, "convert", "CONVERTED"));
        assertFalse(evaluator.matches("lead.status", ConditionOperator.CONTAINS, "lost", "CONVERTED"));
        assertFalse(evaluator.matches("lead.status", ConditionOperator.CONTAINS, null, "CONVERTED"));
    }

    @Test
    void isNullMatchesOnlyAbsentValues() {
        assertTrue(evaluator.matches("field", ConditionOperator.IS_NULL, null, null));
        assertFalse(evaluator.matches("field", ConditionOperator.IS_NULL, null, "valor"));
    }

    @Test
    void isNotNullMatchesOnlyPresentValues() {
        assertTrue(evaluator.matches("field", ConditionOperator.IS_NOT_NULL, null, "valor"));
        assertFalse(evaluator.matches("field", ConditionOperator.IS_NOT_NULL, null, null));
    }
}
