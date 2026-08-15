package com.becommerce.crm.infrastructure.workflow.scheduler;

import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowStaleOpportunityScannerTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
    private final StageRepository stageRepository = mock(StageRepository.class);
    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);

    private final WorkflowStaleOpportunityScanner scanner = new WorkflowStaleOpportunityScanner(
            companyRepository, opportunityRepository, stageRepository, activityRepository, eventPublisher);

    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    @Test
    void staleOpportunity_publishesOpportunityStale() {
        UUID oppId = UUID.randomUUID();
        Opportunity stale = Opportunity.reconstitute(oppId, companyId, "Parada", new BigDecimal("1000"),
                contactId, null, null, null, null, OpportunityStatus.OPEN,
                null, null, null, null, LocalDateTime.now().minusDays(10), LocalDateTime.now());
        when(companyRepository.findAll()).thenReturn(List.of(mockCompany()));
        when(opportunityRepository.findByCompanyId(companyId)).thenReturn(List.of(stale));
        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(activityRepository.findLatestActivityAtByOpportunityId(oppId)).thenReturn(Optional.empty());

        scanner.scan();

        org.mockito.ArgumentCaptor<WorkflowTriggerEvent> captor =
                org.mockito.ArgumentCaptor.forClass(WorkflowTriggerEvent.class);
        verify(eventPublisher).publish(captor.capture());
        WorkflowTriggerEvent event = captor.getValue();
        assertTrue(event.trigger() == TriggerEvent.OPPORTUNITY_STALE);
        assertTrue(event.opportunityId().equals(oppId));
        assertTrue(event.eventId().equals(oppId));
        assertTrue(((Long) event.context().get("opportunity.daysWithoutActivity")) == 10L);
    }

    @Test
    void freshOpportunity_doesNotPublish() {
        UUID oppId = UUID.randomUUID();
        Opportunity fresh = Opportunity.reconstitute(oppId, companyId, "Nova", new BigDecimal("1000"),
                contactId, null, null, null, null, OpportunityStatus.OPEN,
                null, null, null, null, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        when(companyRepository.findAll()).thenReturn(List.of(mockCompany()));
        when(opportunityRepository.findByCompanyId(companyId)).thenReturn(List.of(fresh));
        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(activityRepository.findLatestActivityAtByOpportunityId(oppId)).thenReturn(Optional.empty());

        scanner.scan();

        verify(eventPublisher, never()).publish(any(WorkflowTriggerEvent.class));
    }

    @Test
    void nonOpenOpportunity_isIgnored() {
        UUID oppId = UUID.randomUUID();
        Opportunity won = Opportunity.reconstitute(oppId, companyId, "Ganha", new BigDecimal("1000"),
                contactId, null, null, null, null, OpportunityStatus.WON,
                LocalDateTime.now().minusDays(10), null, null, null,
                LocalDateTime.now().minusDays(15), LocalDateTime.now());
        when(companyRepository.findAll()).thenReturn(List.of(mockCompany()));
        when(opportunityRepository.findByCompanyId(companyId)).thenReturn(List.of(won));
        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of());

        scanner.scan();

        verify(eventPublisher, never()).publish(any(WorkflowTriggerEvent.class));
    }

    private Company mockCompany() {
        return Company.reconstitute(
                companyId, "Empresa Ltda", "Empresa", "00.000.000/0000-00",
                "1", "1", "empresa@teste.com", "11999999999", "www.empresa.com",
                null, null, null, null, null, null, "SP", "BR",
                com.becommerce.crm.domain.company.CompanyPlan.STARTER,
                com.becommerce.crm.domain.company.CompanyStatus.ACTIVE,
                5, 1024, 100, null, null, LocalDateTime.now(), null);
    }
}