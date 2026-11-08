package com.becommerce.crm.billing.application;

import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.masterdata.company.domain.event.CompanyCreatedEvent;
import com.becommerce.crm.masterdata.company.domain.event.CompanyUpdatedEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.mockito.Mockito.*;

class BillingCompanyListenerTest {
    private final BillingService service = mock(BillingService.class);
    private final UUID id = UUID.randomUUID();

    @Test
    void createdActivatesWhenConfigured() {
        new BillingCompanyListener(service, new BillingProperties(true, "u", "KEY", "W"))
                .onCreated(new CompanyCreatedEvent(id, "C", "1", "a@b.com", LocalDateTime.now()));
        verify(service).activate(id);
    }

    @Test
    void createdDoesNothingWhenDisabled() {
        new BillingCompanyListener(service, new BillingProperties(false, "u", "", ""))
                .onCreated(new CompanyCreatedEvent(id, "C", "1", "a@b.com", LocalDateTime.now()));
        verifyNoInteractions(service);
    }

    @Test
    void createdSwallowsRuleErrors() {
        doThrow(new BillingRuleException("sem documento")).when(service).activate(id);
        new BillingCompanyListener(service, new BillingProperties(true, "u", "KEY", "W"))
                .onCreated(new CompanyCreatedEvent(id, "C", null, "a@b.com", LocalDateTime.now()));
        verify(service).activate(id);
    }

    @Test
    void updatedForwardsPlan() {
        new BillingCompanyListener(service, new BillingProperties(false, "u", "", ""))
                .onUpdated(new CompanyUpdatedEvent(id, "C", "1", CompanyPlan.BUSINESS, LocalDateTime.now()));
        verify(service).onPlanChanged(id, CompanyPlan.BUSINESS);
    }
}
