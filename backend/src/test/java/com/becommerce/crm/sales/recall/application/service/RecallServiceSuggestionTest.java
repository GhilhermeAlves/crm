package com.becommerce.crm.sales.recall.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.recall.application.dto.RecallSuggestionResponse;
import com.becommerce.crm.sales.recall.application.port.out.RecallRepository;
import com.becommerce.crm.sales.recall.application.port.out.RecallRepository.SuggestionCandidate;
import com.becommerce.crm.sales.recall.application.port.out.RecallSuggestionDismissalRepository;
import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecallServiceSuggestionTest {

    private final RecallRepository repository = mock(RecallRepository.class);
    private final RecallSuggestionDismissalRepository dismissalRepository =
            mock(RecallSuggestionDismissalRepository.class);
    private final TenantAuditRecorder auditor = mock(TenantAuditRecorder.class);
    private final RecallService service = new RecallService(repository, dismissalRepository, auditor,
            org.mockito.Mockito.mock(com.becommerce.crm.sales.tag.application.port.in.TagUseCase.class));

    private final UUID companyId = UUID.randomUUID();

    private SuggestionCandidate candidate(boolean hasFutureAppointment, boolean hasPendingRecall,
                                          boolean dismissed) {
        return new SuggestionCandidate(UUID.randomUUID(), "Maria", "Silva", "11999998888", "1133334444",
                UUID.randomUUID(), Instant.now(), List.of("Limpeza", "Restauração"),
                hasFutureAppointment, hasPendingRecall, dismissed);
    }

    @Test
    void excludesCandidatesWithFutureAppointment() {
        when(repository.findSuggestionCandidates(eq(companyId), any(), any()))
                .thenReturn(List.of(candidate(true, false, false), candidate(false, false, false)));

        List<RecallSuggestionResponse> suggestions = service.suggestions(companyId, 3);

        assertEquals(1, suggestions.size());
    }

    @Test
    void excludesCandidatesWithPendingRecall() {
        when(repository.findSuggestionCandidates(eq(companyId), any(), any()))
                .thenReturn(List.of(candidate(false, true, false), candidate(false, false, false)));

        assertEquals(1, service.suggestions(companyId, 3).size());
    }

    @Test
    void excludesDismissedCandidates() {
        when(repository.findSuggestionCandidates(eq(companyId), any(), any()))
                .thenReturn(List.of(candidate(false, false, true), candidate(false, false, false)));

        assertEquals(1, service.suggestions(companyId, 3).size());
    }

    @Test
    void keepsCleanCandidateAndMapsResponse() {
        SuggestionCandidate clean = candidate(false, false, false);
        when(repository.findSuggestionCandidates(eq(companyId), any(), any())).thenReturn(List.of(clean));

        List<RecallSuggestionResponse> suggestions = service.suggestions(companyId, 6);

        assertEquals(1, suggestions.size());
        RecallSuggestionResponse response = suggestions.get(0);
        assertEquals(clean.contactId(), response.contactId());
        assertEquals("Maria Silva", response.contactName());
        assertEquals(clean.hostId(), response.hostId());
        assertEquals(List.of("Limpeza", "Restauração"), response.recentProcedures());
    }

    @Test
    void mapsEmptyProceduresToEmptyList() {
        when(repository.findSuggestionCandidates(eq(companyId), any(), any()))
                .thenReturn(List.of(new SuggestionCandidate(UUID.randomUUID(), "João", null, null, null,
                        UUID.randomUUID(), Instant.now(), List.of(), false, false, false)));

        List<RecallSuggestionResponse> suggestions = service.suggestions(companyId, 1);

        assertEquals("João", suggestions.get(0).contactName());
        assertEquals(List.of(), suggestions.get(0).recentProcedures());
    }

    @Test
    void rejectsUnsupportedSince() {
        assertThrows(RecallValidationException.class, () -> service.suggestions(companyId, 2));
        assertThrows(RecallValidationException.class, () -> service.suggestions(companyId, 0));
        verify(repository, never()).findSuggestionCandidates(any(), any(), any());
    }

    @Test
    void dismissSuggestionSkipsInsertWhenAlreadyDismissed() {
        UUID contactId = UUID.randomUUID();
        when(dismissalRepository.existsByCompanyAndContact(companyId, contactId)).thenReturn(true);

        service.dismissSuggestion(companyId, contactId, UUID.randomUUID());

        verify(dismissalRepository, never()).save(any(), any(), any());
    }

    @Test
    void dismissSuggestionInsertsWhenNew() {
        UUID contactId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(dismissalRepository.existsByCompanyAndContact(companyId, contactId)).thenReturn(false);

        service.dismissSuggestion(companyId, contactId, userId);

        verify(dismissalRepository).save(companyId, contactId, userId);
    }

    @Test
    void dismissSuggestionRejectsMissingContact() {
        assertThrows(RecallValidationException.class, () -> service.dismissSuggestion(companyId, null, null));
    }
}
