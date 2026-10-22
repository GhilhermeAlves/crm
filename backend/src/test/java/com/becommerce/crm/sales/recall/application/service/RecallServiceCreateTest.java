package com.becommerce.crm.sales.recall.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.sales.recall.application.dto.CreateRecallRequest;
import com.becommerce.crm.sales.recall.application.dto.RecallResponse;
import com.becommerce.crm.sales.recall.application.port.out.RecallRepository;
import com.becommerce.crm.sales.recall.application.port.out.RecallSuggestionDismissalRepository;
import com.becommerce.crm.sales.recall.domain.Recall;
import com.becommerce.crm.sales.recall.domain.RecallDueUnit;
import com.becommerce.crm.sales.recall.domain.RecallStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecallServiceCreateTest {

    private final RecallRepository repository = mock(RecallRepository.class);
    private final RecallSuggestionDismissalRepository dismissalRepository =
            mock(RecallSuggestionDismissalRepository.class);
    private final TenantAuditRecorder auditor = mock(TenantAuditRecorder.class);
    private final RecallService service = new RecallService(repository, dismissalRepository, auditor,
            org.mockito.Mockito.mock(com.becommerce.crm.sales.tag.application.port.in.TagUseCase.class));

    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID createdBy = UUID.randomUUID();

    @Test
    void createsRecallWithDueDateCalculatedFromToday() {
        when(repository.save(any(Recall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecallResponse response = service.create(companyId,
                new CreateRecallRequest(contactId, "(11) 99999-8888", hostId, "Limpeza", 2, RecallDueUnit.WEEKS, null),
                createdBy);

        ArgumentCaptor<Recall> captor = ArgumentCaptor.forClass(Recall.class);
        verify(repository).save(captor.capture());
        Recall saved = captor.getValue();

        assertEquals(RecallStatus.PENDING, saved.getStatus());
        assertEquals(LocalDate.now().plusWeeks(2), saved.getDueDate());
        assertEquals(contactId, saved.getContactId());
        assertEquals(companyId, saved.getCompanyId());
        assertEquals(createdBy, saved.getCreatedBy());
        assertEquals(RecallStatus.PENDING, response.status());
    }

    @Test
    void createsRecallWithDueDateInMonths() {
        when(repository.save(any(Recall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(companyId,
                new CreateRecallRequest(contactId, "11999998888", hostId, null, 6, RecallDueUnit.MONTHS, null),
                createdBy);

        ArgumentCaptor<Recall> captor = ArgumentCaptor.forClass(Recall.class);
        verify(repository).save(captor.capture());
        assertEquals(LocalDate.now().plusMonths(6), captor.getValue().getDueDate());
    }

    @Test
    void recordsAuditOnCreate() {
        when(repository.save(any(Recall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(companyId,
                new CreateRecallRequest(contactId, "11999998888", hostId, null, 1, RecallDueUnit.DAYS, null),
                createdBy);

        verify(auditor).record(eq(companyId), eq(AuditAction.CREATE), eq(AuditModule.SCHEDULING),
                eq("Recall"), any(), any(), eq(createdBy), any());
    }
}
