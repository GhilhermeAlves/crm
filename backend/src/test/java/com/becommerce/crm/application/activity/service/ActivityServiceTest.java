package com.becommerce.crm.application.activity.service;

import com.becommerce.crm.application.activity.dto.CreateActivityRequest;
import com.becommerce.crm.application.activity.dto.UpdateActivityRequest;
import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.domain.activity.Activity;
import com.becommerce.crm.domain.activity.ActivityType;
import com.becommerce.crm.domain.activity.exception.ActivityNotFoundException;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
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
                null, null, LocalDateTime.now(), LocalDateTime.now(), null);
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
                "ana@e.com", null, null, LocalDateTime.now(), LocalDateTime.now(), null);
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
