package com.becommerce.crm.reports.export.infrastructure.scheduler;

import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.masterdata.storage.domain.exception.StorageObjectNotFoundException;
import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.application.service.ReportExportAuditor;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExportRetentionJobTest {

    private final UUID companyA = UUID.randomUUID();
    private final UUID companyB = UUID.randomUUID();

    private ReportExportRepository repository;
    private StorageUseCase storage;
    private JdbcTemplate jdbc;
    private ReportExportAuditor auditor;
    private ReportExportRetentionJob job;

    @BeforeEach
    void setUp() {
        repository = mock(ReportExportRepository.class);
        storage = mock(StorageUseCase.class);
        jdbc = mock(JdbcTemplate.class);
        auditor = mock(ReportExportAuditor.class);
        job = new ReportExportRetentionJob(repository, storage, jdbc, auditor);
        when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(companyA, companyB));
    }

    private ReportExport ready(UUID companyId) {
        ReportExport export = ReportExport.pending(companyId, UUID.randomUUID(), ReportExportType.CASH_FLOW,
                ExportFormat.PDF, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        export.markReady(UUID.randomUUID(), "fluxo.pdf");
        return export;
    }

    @Test
    void deletesFileAndMarksExpiredPerCompany() {
        ReportExport a = ready(companyA);
        UUID objectA = a.storageObjectId();
        ReportExport b = ready(companyB);
        UUID objectB = b.storageObjectId();
        when(repository.findReadyFinishedBefore(eq(companyA), any(), anyInt())).thenReturn(List.of(a));
        when(repository.findReadyFinishedBefore(eq(companyB), any(), anyInt())).thenReturn(List.of(b));

        job.purge();

        verify(storage).delete(companyA, objectA);
        verify(storage).delete(companyB, objectB);
        verify(repository).update(a);
        verify(repository).update(b);
        verify(auditor).record(ReportExportAuditor.Event.EXPIRED, a);
        verify(auditor).record(ReportExportAuditor.Event.EXPIRED, b);
        assertThat(a.status()).isEqualTo(ReportExportStatus.EXPIRED);
        assertThat(b.status()).isEqualTo(ReportExportStatus.EXPIRED);
    }

    @Test
    void usesSevenDayCutoff() {
        when(repository.findReadyFinishedBefore(any(), any(), anyInt())).thenReturn(List.of());
        Instant before = Instant.now().minus(ReportExportRetentionJob.RETENTION);

        int expired = job.purgeCompany(companyA, before);

        assertThat(expired).isZero();
        verify(repository).findReadyFinishedBefore(companyA, before, ReportExportRetentionJob.BATCH);
    }

    @Test
    void failureOnOneItemDoesNotStopOthers() {
        ReportExport broken = ready(companyA);
        ReportExport ok = ready(companyA);
        when(repository.findReadyFinishedBefore(eq(companyA), any(), anyInt())).thenReturn(List.of(broken, ok));
        when(repository.findReadyFinishedBefore(eq(companyB), any(), anyInt())).thenReturn(List.of());
        doThrow(new RuntimeException("storage fora")).when(storage).delete(companyA, broken.storageObjectId());

        job.purge();

        assertThat(broken.status()).isEqualTo(ReportExportStatus.READY);
        verify(repository, never()).update(broken);
        assertThat(ok.status()).isEqualTo(ReportExportStatus.EXPIRED);
        verify(repository).update(ok);
    }

    @Test
    void missingFileStillExpires() {
        ReportExport export = ready(companyA);
        when(repository.findReadyFinishedBefore(eq(companyA), any(), anyInt())).thenReturn(List.of(export));
        when(repository.findReadyFinishedBefore(eq(companyB), any(), anyInt())).thenReturn(List.of());
        doThrow(new StorageObjectNotFoundException(export.storageObjectId()))
                .when(storage).delete(companyA, export.storageObjectId());

        job.purge();

        assertThat(export.status()).isEqualTo(ReportExportStatus.EXPIRED);
        verify(repository).update(export);
    }

    @Test
    void failureOnOneCompanyDoesNotStopOthers() {
        ReportExport b = ready(companyB);
        when(repository.findReadyFinishedBefore(eq(companyA), any(), anyInt()))
                .thenThrow(new RuntimeException("db"));
        when(repository.findReadyFinishedBefore(eq(companyB), any(), anyInt())).thenReturn(List.of(b));

        job.purge();

        assertThat(b.status()).isEqualTo(ReportExportStatus.EXPIRED);
    }
}
