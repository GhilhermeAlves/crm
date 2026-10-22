package com.becommerce.crm.sales.recall.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.recall.application.dto.RecallResponse;
import com.becommerce.crm.sales.recall.application.dto.ScheduleRecallRequest;
import com.becommerce.crm.sales.recall.application.port.out.RecallRepository;
import com.becommerce.crm.sales.recall.application.port.out.RecallSuggestionDismissalRepository;
import com.becommerce.crm.sales.recall.domain.Recall;
import com.becommerce.crm.sales.recall.domain.RecallStatus;
import com.becommerce.crm.sales.recall.domain.exception.RecallNotFoundException;
import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecallServiceScheduleDismissTest {

    private final RecallRepository repository = mock(RecallRepository.class);
    private final RecallSuggestionDismissalRepository dismissalRepository =
            mock(RecallSuggestionDismissalRepository.class);
    private final TenantAuditRecorder auditor = mock(TenantAuditRecorder.class);
    private final RecallService service = new RecallService(repository, dismissalRepository, auditor,
            org.mockito.Mockito.mock(com.becommerce.crm.sales.tag.application.port.in.TagUseCase.class));

    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();

    private Recall pendingRecall() {
        return Recall.create(companyId, contactId, "11999998888", hostId, null,
                LocalDate.now().plusDays(5), UUID.randomUUID());
    }

    @Test
    void scheduleMarksRecallAsScheduled() {
        Recall recall = pendingRecall();
        UUID appointmentId = UUID.randomUUID();
        when(repository.findById(recall.getId())).thenReturn(Optional.of(recall));
        when(repository.save(any(Recall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecallResponse response = service.schedule(companyId, recall.getId(),
                new ScheduleRecallRequest(appointmentId));

        assertEquals(RecallStatus.SCHEDULED, response.status());
        assertEquals(appointmentId, response.appointmentId());
    }

    @Test
    void scheduleRejectsRecallFromAnotherCompany() {
        Recall foreign = Recall.create(UUID.randomUUID(), contactId, "11999998888", hostId, null,
                LocalDate.now().plusDays(5), UUID.randomUUID());
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(RecallNotFoundException.class, () -> service.schedule(companyId, foreign.getId(),
                new ScheduleRecallRequest(UUID.randomUUID())));
        verify(repository, never()).save(any(Recall.class));
    }

    @Test
    void scheduleRejectsDismissedRecall() {
        Recall recall = pendingRecall();
        recall.dismiss();
        when(repository.findById(recall.getId())).thenReturn(Optional.of(recall));

        assertThrows(RecallValidationException.class, () -> service.schedule(companyId, recall.getId(),
                new ScheduleRecallRequest(UUID.randomUUID())));
    }

    @Test
    void dismissMarksRecallAsDismissed() {
        Recall recall = pendingRecall();
        when(repository.findById(recall.getId())).thenReturn(Optional.of(recall));
        when(repository.save(any(Recall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecallResponse response = service.dismiss(companyId, recall.getId());

        assertEquals(RecallStatus.DISMISSED, response.status());
        assertNull(response.appointmentId());
    }

    @Test
    void dismissRejectsRecallFromAnotherCompany() {
        Recall foreign = Recall.create(UUID.randomUUID(), contactId, "11999998888", hostId, null,
                LocalDate.now().plusDays(5), UUID.randomUUID());
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(RecallNotFoundException.class, () -> service.dismiss(companyId, foreign.getId()));
        verify(repository, never()).save(any(Recall.class));
    }
}
