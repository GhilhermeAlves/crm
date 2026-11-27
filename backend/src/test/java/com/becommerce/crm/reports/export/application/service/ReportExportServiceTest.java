package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.storage.application.dto.StorageDownload;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.reports.export.application.dto.ReportExportRequest;
import com.becommerce.crm.reports.export.application.dto.ReportExportResponse;
import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.reports.export.domain.exception.ReportExportConflictException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportNotFoundException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportRateLimitException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExportServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final List<String> FINANCE = List.of("finance:read");

    private ReportExportRepository repository;
    private StorageUseCase storage;
    private ApplicationEventPublisher events;
    private ReportExportAuditor auditor;
    private ReportExportService service;

    @BeforeEach
    void setUp() {
        repository = mock(ReportExportRepository.class);
        storage = mock(StorageUseCase.class);
        events = mock(ApplicationEventPublisher.class);
        auditor = mock(ReportExportAuditor.class);
        service = new ReportExportService(repository, storage, events, auditor);
    }

    @Test
    void dreRequestUsesWholeMonthAndPublishesEvent() {
        ReportExportResponse response = service.request(COMPANY, USER, FINANCE,
                new ReportExportRequest("monthly_dre", "pdf", null, null, "2026-06"));

        ArgumentCaptor<ReportExport> saved = ArgumentCaptor.forClass(ReportExport.class);
        verify(repository).insert(saved.capture());
        assertThat(saved.getValue().periodFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(saved.getValue().periodTo()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(response.status()).isEqualTo("PENDING");
        verify(events).publishEvent(new ReportExportRequestedEvent(COMPANY, response.id()));
        verify(auditor).record(ReportExportAuditor.Event.REQUESTED, saved.getValue());
    }

    @Test
    void missingPermissionIsDenied() {
        assertThatThrownBy(() -> service.request(COMPANY, USER, List.of("finance:read"),
                new ReportExportRequest("AGENDA", "XLSX", "2026-06-01", "2026-06-30", null)))
                .isInstanceOf(CrmAccessDeniedException.class);
        verify(repository, never()).insert(any());
    }

    @Test
    void invalidPeriodsAreRejected() {
        assertThatThrownBy(() -> service.request(COMPANY, USER, FINANCE,
                new ReportExportRequest("CASH_FLOW", "PDF", "2026-06-30", "2026-06-01", null)))
                .isInstanceOf(ReportExportValidationException.class);
        assertThatThrownBy(() -> service.request(COMPANY, USER, FINANCE,
                new ReportExportRequest("CASH_FLOW", "PDF", "2025-01-01", "2026-01-01", null)))
                .isInstanceOf(ReportExportValidationException.class)
                .hasMessageContaining("12 meses");
        assertThatThrownBy(() -> service.request(COMPANY, USER, FINANCE,
                new ReportExportRequest("CASH_FLOW", "DOCX", "2026-06-01", "2026-06-30", null)))
                .isInstanceOf(ReportExportValidationException.class);
    }

    @Test
    void downloadOfAnotherUsersExportIsNotFound() {
        ReportExport export = ReportExport.pending(COMPANY, UUID.randomUUID(), ReportExportType.CASH_FLOW,
                ExportFormat.PDF, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        export.markReady(UUID.randomUUID(), "f.pdf");
        when(repository.findById(COMPANY, export.id())).thenReturn(Optional.of(export));

        assertThatThrownBy(() -> service.download(COMPANY, USER, FINANCE, export.id()))
                .isInstanceOf(ReportExportNotFoundException.class);
        verify(storage, never()).download(any(), any());
        verify(auditor).record(ReportExportAuditor.Event.ACCESS_DENIED, export, USER);
    }

    @Test
    void downloadWithoutPermissionIsAuditedAsDenied() {
        ReportExport export = ReportExport.pending(COMPANY, USER, ReportExportType.AGENDA, ExportFormat.PDF,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        export.markReady(UUID.randomUUID(), "agenda.pdf");
        when(repository.findById(COMPANY, export.id())).thenReturn(Optional.of(export));

        assertThatThrownBy(() -> service.download(COMPANY, USER, FINANCE, export.id()))
                .isInstanceOf(CrmAccessDeniedException.class);
        verify(auditor).record(ReportExportAuditor.Event.ACCESS_DENIED, export, USER);
        verify(storage, never()).download(any(), any());
    }

    @Test
    void downloadOfPendingExportIsNotFound() {
        ReportExport export = ReportExport.pending(COMPANY, USER, ReportExportType.CASH_FLOW, ExportFormat.PDF,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        when(repository.findById(COMPANY, export.id())).thenReturn(Optional.of(export));

        assertThatThrownBy(() -> service.download(COMPANY, USER, FINANCE, export.id()))
                .isInstanceOf(ReportExportNotFoundException.class);
    }

    @Test
    void downloadReturnsStoredFileWithExportName() {
        UUID objectId = UUID.randomUUID();
        ReportExport export = ReportExport.pending(COMPANY, USER, ReportExportType.CASH_FLOW, ExportFormat.XLSX,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        export.markReady(objectId, "fluxo-de-caixa_2026-06-01_a_2026-06-30.xlsx");
        when(repository.findById(COMPANY, export.id())).thenReturn(Optional.of(export));
        when(storage.download(COMPANY, objectId))
                .thenReturn(new StorageDownload(objectId, "x", "application/octet-stream", 3, new byte[] {1, 2, 3}));

        StorageDownload file = service.download(COMPANY, USER, FINANCE, export.id());

        assertThat(file.fileName()).isEqualTo("fluxo-de-caixa_2026-06-01_a_2026-06-30.xlsx");
        assertThat(file.contentType()).isEqualTo(ExportFormat.XLSX.contentType());
        assertThat(file.data()).hasSize(3);
        verify(auditor).record(ReportExportAuditor.Event.DOWNLOADED, export, USER);
    }

    private static final ReportExportRequest DRE =
            new ReportExportRequest("MONTHLY_DRE", "PDF", null, null, "2026-06");

    private ReportExport stored(UUID owner) {
        ReportExport export = ReportExport.pending(COMPANY, owner, ReportExportType.CASH_FLOW, ExportFormat.PDF,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        when(repository.findById(COMPANY, export.id())).thenReturn(Optional.of(export));
        return export;
    }

    @Test
    void tooManyPendingIsRateLimited() {
        when(repository.countPending(COMPANY, USER)).thenReturn(3);

        assertThatThrownBy(() -> service.request(COMPANY, USER, FINANCE, DRE))
                .isInstanceOf(ReportExportRateLimitException.class)
                .hasMessageContaining("3 relatórios sendo gerados");
        verify(repository, never()).insert(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void tooManyPerHourIsRateLimited() {
        when(repository.countRequestedSince(eq(COMPANY), eq(USER), any())).thenReturn(30);

        assertThatThrownBy(() -> service.request(COMPANY, USER, FINANCE, DRE))
                .isInstanceOf(ReportExportRateLimitException.class)
                .hasMessageContaining("30 relatórios por hora");
        verify(repository, never()).insert(any());
    }

    @Test
    void limitsAreConfigurable() {
        service = new ReportExportService(repository, storage, events, auditor, 1, 100);
        when(repository.countPending(COMPANY, USER)).thenReturn(1);

        assertThatThrownBy(() -> service.request(COMPANY, USER, FINANCE, DRE))
                .isInstanceOf(ReportExportRateLimitException.class);
    }

    @Test
    void belowLimitsIsAccepted() {
        when(repository.countPending(COMPANY, USER)).thenReturn(2);
        when(repository.countRequestedSince(eq(COMPANY), eq(USER), any())).thenReturn(29);

        service.request(COMPANY, USER, FINANCE, DRE);

        verify(repository).insert(any());
    }

    @Test
    void ownerCancelsPendingExport() {
        ReportExport export = stored(USER);
        when(repository.updateIfPending(export)).thenReturn(true);

        ReportExportResponse response = service.cancel(COMPANY, USER, export.id());

        assertThat(response.status()).isEqualTo("CANCELED");
        assertThat(export.status()).isEqualTo(ReportExportStatus.CANCELED);
        verify(auditor).record(ReportExportAuditor.Event.CANCELED, export);
    }

    @Test
    void cancelOfAnotherUsersExportIsNotFound() {
        ReportExport export = stored(UUID.randomUUID());

        assertThatThrownBy(() -> service.cancel(COMPANY, USER, export.id()))
                .isInstanceOf(ReportExportNotFoundException.class);
        verify(repository, never()).updateIfPending(any());
        verify(auditor).record(ReportExportAuditor.Event.ACCESS_DENIED, export, USER);
    }

    @Test
    void cancelOfMissingExportIsNotFound() {
        assertThatThrownBy(() -> service.cancel(COMPANY, USER, UUID.randomUUID()))
                .isInstanceOf(ReportExportNotFoundException.class);
    }

    @Test
    void cancelOfFinishedExportIsConflict() {
        ReportExport export = stored(USER);
        export.markReady(UUID.randomUUID(), "f.pdf");

        assertThatThrownBy(() -> service.cancel(COMPANY, USER, export.id()))
                .isInstanceOf(ReportExportConflictException.class);
        verify(repository, never()).updateIfPending(any());
    }

    @Test
    void cancelLosingRaceWithGeneratorIsConflict() {
        ReportExport export = stored(USER);
        when(repository.updateIfPending(export)).thenReturn(false);

        assertThatThrownBy(() -> service.cancel(COMPANY, USER, export.id()))
                .isInstanceOf(ReportExportConflictException.class);
    }

    @Test
    void catalogListsOnlyAllowedReports() {
        assertThat(service.catalog(List.of(), FINANCE))
                .extracting(entry -> entry.type())
                .containsExactly("MONTHLY_DRE", "CASH_FLOW", "TRANSACTIONS");
        assertThat(service.catalog(List.of(), List.of())).isEmpty();
    }

    @Test
    void catalogShowsEverythingToCrmAdmin() {
        assertThat(service.catalog(List.of("CRM_ADMIN"), List.of()))
                .hasSize(ReportExportType.values().length)
                .anySatisfy(entry -> {
                    assertThat(entry.type()).isEqualTo("MONTHLY_DRE");
                    assertThat(entry.slug()).isEqualTo("monthly-dre");
                    assertThat(entry.periodMode()).isEqualTo("MONTH");
                    assertThat(entry.formats()).containsExactly("PDF", "XLSX", "CSV");
                    assertThat(entry.domain()).isEqualTo("FINANCE");
                });
    }

    @Test
    void crmAdminRequestsWithoutExplicitPermission() {
        service.request(COMPANY, USER, List.of("CRM_ADMIN"), List.of(),
                new ReportExportRequest("AGENDA", "CSV", "2026-01-01", "2026-01-31", null));

        verify(repository).insert(any());
    }

    @Test
    void rangeWindowComesFromCatalog() {
        assertThatThrownBy(() -> service.request(COMPANY, USER, List.of("contact:read"),
                new ReportExportRequest("BUDGETS", "XLSX", "2026-01-01", "2027-01-01", null)))
                .isInstanceOf(ReportExportValidationException.class)
                .hasMessageContaining(ReportExportType.BUDGETS.maxMonths() + " meses");
    }
}
