package com.becommerce.crm.sales.recall.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.recall.application.dto.RecallResponse;
import com.becommerce.crm.sales.recall.application.dto.UpdateRecallRequest;
import com.becommerce.crm.sales.recall.application.port.out.RecallRepository;
import com.becommerce.crm.sales.recall.application.port.out.RecallSuggestionDismissalRepository;
import com.becommerce.crm.sales.recall.domain.Recall;
import com.becommerce.crm.sales.recall.domain.RecallDueUnit;
import com.becommerce.crm.sales.recall.domain.RecallStatus;
import com.becommerce.crm.sales.recall.domain.exception.RecallNotFoundException;
import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;
import com.becommerce.crm.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecallServiceListTest {

    private final RecallRepository repository = mock(RecallRepository.class);
    private final RecallSuggestionDismissalRepository dismissalRepository =
            mock(RecallSuggestionDismissalRepository.class);
    private final TenantAuditRecorder auditor = mock(TenantAuditRecorder.class);
    private final RecallService service = new RecallService(repository, dismissalRepository, auditor,
            org.mockito.Mockito.mock(com.becommerce.crm.sales.tag.application.port.in.TagUseCase.class));

    private final UUID companyId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    private Recall recall(UUID companyId) {
        return Recall.create(companyId, contactId, "11999998888", hostId, "Limpeza",
                LocalDate.now().plusDays(3), UUID.randomUUID());
    }

    @Test
    void todayWindowPassesHalfOpenRangeForToday() {
        when(repository.findByCompany(any(), any(), any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new RecallRepository.PageResult(List.of(), 0));

        service.list(companyId, null, null, null, null, "TODAY", 0, 20);

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).findByCompany(eq(companyId), isNull(), isNull(), isNull(), isNull(),
                from.capture(), to.capture(), eq(0), eq(20));
        assertEquals(LocalDate.now(), from.getValue());
        assertEquals(LocalDate.now().plusDays(1), to.getValue());
    }

    @Test
    void overdueWindowEndsAtTodayExclusive() {
        when(repository.findByCompany(any(), any(), any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new RecallRepository.PageResult(List.of(), 0));

        service.list(companyId, null, null, null, null, "OVERDUE", 0, 20);

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).findByCompany(eq(companyId), isNull(), isNull(), isNull(), isNull(),
                from.capture(), to.capture(), eq(0), eq(20));
        assertNull(from.getValue());
        assertEquals(LocalDate.now(), to.getValue());
    }

    @Test
    void defaultsToNextSevenDaysWhenWindowIsBlank() {
        when(repository.findByCompany(any(), any(), any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new RecallRepository.PageResult(List.of(), 0));

        service.list(companyId, null, null, null, null, null, 0, 20);

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).findByCompany(eq(companyId), isNull(), isNull(), isNull(), isNull(),
                from.capture(), to.capture(), eq(0), eq(20));
        assertEquals(LocalDate.now(), from.getValue());
        assertEquals(LocalDate.now().plusDays(8), to.getValue());
    }

    @Test
    void normalizesSearchAndTagFilters() {
        when(repository.findByCompany(any(), any(), any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new RecallRepository.PageResult(List.of(), 0));

        service.list(companyId, hostId, "  Limpeza ", null, "  Maria  ", "ALL", 0, 20);

        verify(repository).findByCompany(eq(companyId), eq(hostId), eq("limpeza"), isNull(), eq("%maria%"),
                isNull(), isNull(), eq(0), eq(20));
    }

    @Test
    void rejectsUnknownWindow() {
        assertThrows(RecallValidationException.class,
                () -> service.list(companyId, null, null, null, null, "SOMETIME", 0, 20));
        verify(repository, never()).findByCompany(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void mapsContentIntoPageResponse() {
        Recall stored = recall(companyId);
        when(repository.findByCompany(any(), any(), any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new RecallRepository.PageResult(List.of(stored), 1));

        PageResponse<RecallResponse> page = service.list(companyId, null, null, null, null, "NEXT_7_DAYS", 0, 20);

        assertEquals(1, page.totalElements());
        assertEquals(1, page.content().size());
        assertEquals(stored.getId(), page.content().get(0).id());
        assertEquals(RecallStatus.PENDING, page.content().get(0).status());
    }

    @Test
    void updateRejectsRecallFromAnotherCompany() {
        Recall foreign = recall(UUID.randomUUID());
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(RecallNotFoundException.class, () -> service.update(companyId, foreign.getId(),
                new UpdateRecallRequest(null, null, null, null, null, null)));
        verify(repository, never()).save(any(Recall.class));
    }

    @Test
    void updateRequiresAmountAndUnitTogether() {
        Recall stored = recall(companyId);
        when(repository.findById(stored.getId())).thenReturn(Optional.of(stored));

        assertThrows(RecallValidationException.class, () -> service.update(companyId, stored.getId(),
                new UpdateRecallRequest(null, null, null, 2, null, null)));
    }

    @Test
    void updateRecomputesDueDateWhenPrazoIsSent() {
        Recall stored = recall(companyId);
        when(repository.findById(stored.getId())).thenReturn(Optional.of(stored));
        when(repository.save(any(Recall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecallResponse response = service.update(companyId, stored.getId(),
                new UpdateRecallRequest(null, null, null, 10, RecallDueUnit.DAYS, null));

        assertEquals(LocalDate.now().plusDays(10), response.dueDate());
    }
}
