package com.becommerce.crm.automation.workflow.application.service;

import com.becommerce.crm.automation.workflow.domain.ConditionOperator;
import com.becommerce.crm.automation.workflow.domain.TriggerEvent;
import com.becommerce.crm.automation.workflow.domain.Workflow;
import com.becommerce.crm.automation.workflow.domain.WorkflowCondition;
import com.becommerce.crm.automation.workflow.domain.event.WorkflowTriggerEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowConditionEvaluatorTest {

    private final WorkflowConditionEvaluator evaluator = new WorkflowConditionEvaluator();

    private final UUID companyId = UUID.randomUUID();

    @Test
    void workflowWithoutConditions_alwaysMatches() {
        Workflow wf = Workflow.create(companyId, "Sem condição", null, TriggerEvent.OPPORTUNITY_WON);
        assertTrue(evaluator.matches(wf, WorkflowTriggerEvent.opportunityWon(
                companyId, UUID.randomUUID(), null, "Fechado", new BigDecimal("1000"))));
    }

    @Test
    void stageEquals_matchesCaseInsensitive() {
        Workflow wf = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.stage",
                ConditionOperator.EQUALS, "Proposta", 0));

        WorkflowTriggerEvent event = WorkflowTriggerEvent.opportunityStageChanged(
                companyId, UUID.randomUUID(), null, "proposta", new BigDecimal("500"));
        assertTrue(evaluator.matches(wf, event));
    }

    @Test
    void stageEquals_wrongStage_doesNotMatch() {
        Workflow wf = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.stage",
                ConditionOperator.EQUALS, "Proposta", 0));

        WorkflowTriggerEvent event = WorkflowTriggerEvent.opportunityStageChanged(
                companyId, UUID.randomUUID(), null, "Qualificação", new BigDecimal("500"));
        assertFalse(evaluator.matches(wf, event));
    }

    @Test
    void valueGreaterThan_matches() {
        Workflow wf = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_CREATED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.value",
                ConditionOperator.GREATER_THAN, "10000", 0));

        WorkflowTriggerEvent event = WorkflowTriggerEvent.opportunityCreated(
                companyId, UUID.randomUUID(), null, "Proposta", new BigDecimal("15000"));
        assertTrue(evaluator.matches(wf, event));
    }

    @Test
    void valueLessOrEqual_doesNotMatch_whenAbove() {
        Workflow wf = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_CREATED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.value",
                ConditionOperator.LESS_OR_EQUAL, "10000", 0));

        WorkflowTriggerEvent event = WorkflowTriggerEvent.opportunityCreated(
                companyId, UUID.randomUUID(), null, "Proposta", new BigDecimal("25000"));
        assertFalse(evaluator.matches(wf, event));
    }

    @Test
    void missingField_doesNotMatch() {
        Workflow wf = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_CREATED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "task.priority",
                ConditionOperator.EQUALS, "HIGH", 0));

        WorkflowTriggerEvent event = WorkflowTriggerEvent.opportunityCreated(
                companyId, UUID.randomUUID(), null, "Proposta", new BigDecimal("500"));
        assertFalse(evaluator.matches(wf, event), "Campo ausente no contexto ⇒ condição falsa");
    }

    @Test
    void multipleConditions_allMustMatch() {
        Workflow wf = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.stage",
                ConditionOperator.EQUALS, "Proposta", 0));
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.value",
                ConditionOperator.GREATER_OR_EQUAL, "10000", 1));

        WorkflowTriggerEvent ok = WorkflowTriggerEvent.opportunityStageChanged(
                companyId, UUID.randomUUID(), null, "Proposta", new BigDecimal("10000"));
        assertTrue(evaluator.matches(wf, ok));

        WorkflowTriggerEvent lowValue = WorkflowTriggerEvent.opportunityStageChanged(
                companyId, UUID.randomUUID(), null, "Proposta", new BigDecimal("500"));
        assertFalse(evaluator.matches(wf, lowValue));
    }

    @Test
    void evaluate_exposesExpectedActualAndMatchPerCondition() {
        Workflow wf = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.stage",
                ConditionOperator.EQUALS, "Proposta", 0));
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.value",
                ConditionOperator.GREATER_OR_EQUAL, "10000", 1));

        WorkflowTriggerEvent event = WorkflowTriggerEvent.opportunityStageChanged(
                companyId, UUID.randomUUID(), null, "Proposta", new BigDecimal("500"));

        var results = evaluator.evaluate(wf, event);
        assertEquals(2, results.size());
        var stage = results.get(0);
        assertEquals("opportunity.stage", stage.field());
        assertEquals("Proposta", stage.expected());
        assertEquals("Proposta", stage.actual());
        assertTrue(stage.matched());
        var value = results.get(1);
        assertEquals(new BigDecimal("10000"), new java.math.BigDecimal(String.valueOf(value.expected())));
        assertEquals(new BigDecimal("500"), new java.math.BigDecimal(String.valueOf(value.actual())));
        assertFalse(value.matched());
    }
}