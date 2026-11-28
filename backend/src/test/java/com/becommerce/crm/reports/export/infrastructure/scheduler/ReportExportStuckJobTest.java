package com.becommerce.crm.reports.export.infrastructure.scheduler;

import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.application.service.ReportExportGenerator;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExportStuckJobTest {

    private final UUID companyA = UUID.randomUUID();
    private final UUID companyB = UUID.randomUUID();

    private ReportExportRepository repository;
    private ReportExportGenerator generator;
    private JdbcTemplate jdbc;
    private ReportExportStuckJob job;

    @BeforeEach
    void setUp() {
        repository = mock(ReportExportRepository.class);
        generator = mock(ReportExportGenerator.class);
        jdbc = mock(JdbcTemplate.class);
        job = new ReportExportStuckJob(repository, generator, jdbc, 30);
        when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(companyA, companyB));
    }

    private ReportExport pending(UUID companyId) {
        return ReportExport.pending(companyId, UUID.randomUUID(), ReportExportType.CASH_FLOW, ExportFormat.PDF,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
    }

    @Test
    void failsStuckExportsOfEachCompanyWithTimeoutMessage() {
        ReportExport a = pending(companyA);
        ReportExport b = pending(companyB);
        when(repository.findPendingCreatedBefore(eq(companyA), any(), anyInt())).thenReturn(List.of(a));
        when(repository.findPendingCreatedBefore(eq(companyB), any(), anyInt())).thenReturn(List.of(b));
        when(generator.failPending(any(), any(), anyString())).thenReturn(true);

        job.failStuck();

        verify(generator).failPending(companyA, a, "Tempo de geração excedido. Tente novamente.");
        verify(generator).failPending(companyB, b, "Tempo de geração excedido. Tente novamente.");
    }

    @Test
    void usesConfiguredTimeoutAsCutoff() {
        job.failStuck();

        ArgumentCaptor<Instant> before = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findPendingCreatedBefore(eq(companyA), before.capture(), anyInt());
        assertThat(before.getValue()).isCloseTo(Instant.now().minus(Duration.ofMinutes(30)),
                within(Duration.ofSeconds(5)));
    }

    @Test
    void failureInOneCompanyDoesNotStopOthers() {
        ReportExport b = pending(companyB);
        when(repository.findPendingCreatedBefore(eq(companyA), any(), anyInt()))
                .thenThrow(new IllegalStateException("db"));
        when(repository.findPendingCreatedBefore(eq(companyB), any(), anyInt())).thenReturn(List.of(b));

        job.failStuck();

        verify(generator).failPending(companyB, b, ReportExportStuckJob.TIMEOUT_MESSAGE);
    }

    @Test
    void countsOnlyExportsActuallyFailed() {
        ReportExport x = pending(companyA);
        ReportExport y = pending(companyA);
        when(repository.findPendingCreatedBefore(eq(companyA), any(), anyInt())).thenReturn(List.of(x, y));
        when(generator.failPending(companyA, x, ReportExportStuckJob.TIMEOUT_MESSAGE)).thenReturn(true);
        when(generator.failPending(companyA, y, ReportExportStuckJob.TIMEOUT_MESSAGE)).thenReturn(false);

        assertThat(job.failCompany(companyA, Instant.now())).isEqualTo(1);
    }
}
