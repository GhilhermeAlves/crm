package com.becommerce.crm.domain.campaign;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Ciclo de vida da campanha (PLAN.md seção 4). */
class CampaignTest {

    private Campaign draft() {
        return Campaign.create(UUID.randomUUID(), "Black Friday", null,
                AudienceType.CONTACTS, null, null, UUID.randomUUID());
    }

    @Test
    void newCampaignStartsAsDraft() {
        assertEquals(CampaignStatus.DRAFT, draft().getStatus());
    }

    @Test
    void fullLifecycleIsValid() {
        Campaign c = draft();
        c.transitionTo(CampaignStatus.SCHEDULED);
        c.transitionTo(CampaignStatus.RUNNING);
        c.transitionTo(CampaignStatus.PAUSED);
        c.transitionTo(CampaignStatus.RUNNING);
        c.transitionTo(CampaignStatus.COMPLETED);
        assertEquals(CampaignStatus.COMPLETED, c.getStatus());
    }

    @Test
    void draftCanCancelDirectly() {
        Campaign c = draft();
        c.transitionTo(CampaignStatus.CANCELLED);
        assertEquals(CampaignStatus.CANCELLED, c.getStatus());
    }

    @Test
    void invalidTransitionsAreRejected() {
        assertThrows(IllegalStateException.class,
                () -> draft().transitionTo(CampaignStatus.RUNNING)); // DRAFT -> RUNNING
        assertThrows(IllegalStateException.class,
                () -> draft().transitionTo(CampaignStatus.COMPLETED)); // DRAFT -> COMPLETED

        Campaign scheduled = draft();
        scheduled.transitionTo(CampaignStatus.SCHEDULED);
        assertThrows(IllegalStateException.class,
                () -> scheduled.transitionTo(CampaignStatus.PAUSED)); // SCHEDULED -> PAUSED

        Campaign done = draft();
        done.transitionTo(CampaignStatus.CANCELLED);
        assertThrows(IllegalStateException.class,
                () -> done.transitionTo(CampaignStatus.RUNNING)); // terminal
    }

    @Test
    void editingAllowedOnlyInDraft() {
        Campaign c = draft();
        c.updateDetails("Novo nome", "desc");
        assertEquals("Novo nome", c.getName());

        c.transitionTo(CampaignStatus.SCHEDULED);
        assertThrows(IllegalStateException.class,
                () -> c.updateDetails("Outro", null));
    }

    @Test
    void createRequiresNameAndAudience() {
        UUID companyId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> Campaign.create(companyId, "", null, AudienceType.CONTACTS, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> Campaign.create(companyId, "Nome", null, null, null, null, null));
    }
}
