package com.becommerce.crm.sales.activity.application.service;

import com.becommerce.crm.sales.activity.application.dto.CreateActivityRequest;
import com.becommerce.crm.sales.activity.application.dto.UpdateActivityRequest;
import com.becommerce.crm.sales.activity.application.port.out.ActivityRepository;
import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.sales.pipeline.application.port.out.OpportunityRepository;
import com.becommerce.crm.sales.activity.domain.Activity;
import com.becommerce.crm.sales.activity.domain.ActivityType;
import com.becommerce.crm.sales.activity.domain.exception.ActivityNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.sales.pipeline.domain.Opportunity;
import com.becommerce.crm.sales.pipeline.domain.OpportunityStatus;
import com.becommerce.crm.sales.pipeline.domain.exception.OpportunityNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock ActivityRepository activityRepository;
    @Mock ContactRepository contactRepository;
    @Mock OpportunityRepository opportunityRepository;
    @Mock TenantAuditRecorder auditor;
    @Mock com.becommerce.crm.identity.application.port.output.EventPublisher eventPublisher;

    @InjectMocks ActivityService activityService;

    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        lenient().when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private Contact ownedContact() {
        return Contact.reconstitute(UUID.randomUUID(), companyId, "Ana", "Souza", "ana@e.com",
                null, null, null,
                null, null, null, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private Opportunity ownedOpportunity() {
        return Opportunity.reconstitute(UUID.randomUUID(), companyId, "Negócio", new BigDecimal("150.00"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null,
                OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Activity activity() {
        return Activity.reconstitute(UUID.randomUUID(), companyId, null, null,
                ActivityType.CALL, "Ligação inicial", "desc", LocalDateTime.now(),
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldCreateActivityWithOwnedLinks() {
        Contact contact = ownedContact();
        when(contactRepository.findById(contact.getId())).thenReturn(Optional.of(contact));

        var response = activityService.create(companyId,
                new CreateActivityRequest(contact.getId(), null, ActivityType.CALL,
                        "Proposta", "detalhes", LocalDateTime.now()), UUID.randomUUID());

        assertEquals(contact.getId(), response.contactId());
        assertEquals("Proposta", response.subject());
        verify(activityRepository).save(any(Activity.class));
        verify(auditor).record(eq(companyId), any(), any(), any(), any(), any(), any(), any());
        assertNull(TenantContext.getCompanyId(), "contexto deve ser limpo");
    }

    @Test
    void shouldRejectContactFromAnotherCompany() {
        Contact foreign = Contact.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "Ana", "Souza",
                "ana@e.com", null, null, null,
                null, null, null, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(contactRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(ContactNotFoundException.class, () -> activityService.create(companyId,
                new CreateActivityRequest(foreign.getId(), null, ActivityType.CALL,
                        "X", null, LocalDateTime.now()), UUID.randomUUID()));
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void shouldRejectOpportunityFromAnotherCompany() {
        Opportunity foreign = Opportunity.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "N",
                new BigDecimal("1.00"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, null, OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(opportunityRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(OpportunityNotFoundException.class, () -> activityService.create(companyId,
                new CreateActivityRequest(null, foreign.getId(), ActivityType.CALL,
                        "X", null, LocalDateTime.now()), UUID.randomUUID()));
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void shouldThrowWhenActivityBelongsToAnotherCompany() {
        Activity foreign = Activity.reconstitute(UUID.randomUUID(), UUID.randomUUID(), null, null,
                ActivityType.CALL, "x", null, LocalDateTime.now(), UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());
        when(activityRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(ActivityNotFoundException.class,
                () -> activityService.getById(companyId, foreign.getId()));
    }

    @Test
    void shouldUpdateOwnedActivity() {
        Activity act = activity();
        when(activityRepository.findById(act.getId())).thenReturn(Optional.of(act));

        var response = activityService.update(companyId, act.getId(),
                new UpdateActivityRequest(ActivityType.MEETING, "Reunião", "nova desc", null));

        assertEquals(ActivityType.MEETING, response.type());
        assertEquals("Reunião", response.subject());
        verify(activityRepository).save(act);
    }

    @Test
    void shouldListByCompanyAndByOpportunity() {
        Activity act = activity();
        Opportunity opp = ownedOpportunity();
        when(activityRepository.findByCompanyId(companyId)).thenReturn(List.of(act));
        when(activityRepository.findByOpportunityId(opp.getId())).thenReturn(List.of(act));
        when(opportunityRepository.findById(opp.getId())).thenReturn(Optional.of(opp));

        assertEquals(1, activityService.listByCompany(companyId).size());
        assertEquals(1, activityService.listByOpportunity(companyId, opp.getId()).size());
    }
}
