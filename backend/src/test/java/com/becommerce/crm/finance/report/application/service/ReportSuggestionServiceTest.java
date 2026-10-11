package com.becommerce.crm.finance.report.application.service;

import com.becommerce.crm.finance.report.application.dto.CreateReportSuggestionRequest;
import com.becommerce.crm.finance.report.application.dto.ReportSuggestionResponse;
import com.becommerce.crm.finance.report.application.port.output.ReportSuggestionRepository;
import com.becommerce.crm.finance.report.domain.exception.ReportSuggestionValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSuggestionServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID SUGGESTION = UUID.randomUUID();

    private ReportSuggestionRepository repository;
    private ReportSuggestionService service;

    @BeforeEach
    void setUp() {
        repository = mock(ReportSuggestionRepository.class);
        service = new ReportSuggestionService(repository);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void createsNormalizingCategoriesAndTrimmingNotes() {
        when(repository.create(any(), anyList(), any(), any())).thenReturn(SUGGESTION);

        ReportSuggestionResponse response = service.create(COMPANY,
                new CreateReportSuggestionRequest(
                        List.of("clinical_record", "FINANCE", "Clinical_Record"),
                        "  quero ver por profissional  "),
                ACTOR);

        assertEquals(SUGGESTION, response.id());
        verify(repository).create(eq(COMPANY), eq(List.of("CLINICAL_RECORD", "FINANCE")),
                eq("quero ver por profissional"), eq(ACTOR));
        assertFalse(TenantContext.hasCompanyId());
    }

    @Test
    void blankNotesBecomeNull() {
        when(repository.create(any(), anyList(), any(), any())).thenReturn(SUGGESTION);

        service.create(COMPANY,
                new CreateReportSuggestionRequest(List.of("OTHER"), "   "), ACTOR);

        verify(repository).create(eq(COMPANY), eq(List.of("OTHER")), isNull(), eq(ACTOR));
    }

    @Test
    void rejectsEmptyCategories() {
        assertThrows(ReportSuggestionValidationException.class, () -> service.create(COMPANY,
                new CreateReportSuggestionRequest(List.of(), null), ACTOR));
        assertThrows(ReportSuggestionValidationException.class, () -> service.create(COMPANY,
                new CreateReportSuggestionRequest(null, null), ACTOR));
        verify(repository, never()).create(any(), anyList(), any(), any());
    }

    @Test
    void rejectsUnknownCategory() {
        assertThrows(ReportSuggestionValidationException.class, () -> service.create(COMPANY,
                new CreateReportSuggestionRequest(List.of("FINANCE", "BOGUS"), null), ACTOR));
        verify(repository, never()).create(any(), anyList(), any(), any());
    }

    @Test
    void rejectsTooLongNotes() {
        String longNotes = "x".repeat(2001);
        assertThrows(ReportSuggestionValidationException.class, () -> service.create(COMPANY,
                new CreateReportSuggestionRequest(List.of("FINANCE"), longNotes), ACTOR));
        verify(repository, never()).create(any(), anyList(), any(), any());
    }

    @Test
    void alwaysClearsTenantContextWhenRepositoryFails() {
        when(repository.create(any(), anyList(), any(), any()))
                .thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> service.create(COMPANY,
                new CreateReportSuggestionRequest(List.of("FINANCE"), null), ACTOR));

        assertNull(TenantContext.getCompanyId());
    }
}
