package com.becommerce.crm.domain.workflow;

import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Novos triggers e eventos da Sprint 18 (Automações Omnichannel). */
class WorkflowTriggerEventSprint18Test {

    @Test
    void contactCreatedCarriesContext() {
        var event = WorkflowTriggerEvent.contactCreated(
                UUID.randomUUID(), UUID.randomUUID(), "a@b.com", "+5511...");
        assertEquals(TriggerEvent.CONTACT_CREATED, event.trigger());
        assertNotNull(event.eventId());
        assertEquals("a@b.com", event.context().get("contact.email"));
    }

    @Test
    void leadStatusChangedCarriesStatuses() {
        var event = WorkflowTriggerEvent.leadStatusChanged(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "NEW", "CONVERTED");
        assertEquals(TriggerEvent.LEAD_STATUS_CHANGED, event.trigger());
        assertEquals("CONVERTED", event.context().get("lead.status"));
        assertEquals("NEW", event.context().get("lead.previousStatus"));
    }

    @Test
    void campaignCompletedHasDeterministicEventId() {
        var companyId = UUID.randomUUID();
        var campaignId = UUID.randomUUID();
        var executionId = UUID.randomUUID();
        var e1 = WorkflowTriggerEvent.campaignCompleted(companyId, campaignId, executionId, 2, 10);
        var e2 = WorkflowTriggerEvent.campaignCompleted(companyId, campaignId, executionId, 2, 10);
        assertEquals(TriggerEvent.CAMPAIGN_COMPLETED, e1.trigger());
        assertEquals(e1.eventId(), e2.eventId(), "eventId determinístico (= executionId)");
        assertEquals(2, ((Number) e1.context().get("campaign.failedCount")).intValue());
        assertTrue(e1.context().containsKey("campaign.totalRecipients"));
    }
}
