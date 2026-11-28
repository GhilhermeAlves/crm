package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReportExportAuditorTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    private final TenantAuditRecorder recorder = mock(TenantAuditRecorder.class);
    // Sem gerenciador de transação: grava direto (o REQUIRES_NEW é da configuração real).
    private final ReportExportAuditor auditor = new ReportExportAuditor(recorder, null);

    private static ReportExport export() {
        return ReportExport.pending(COMPANY, USER, ReportExportType.TRANSACTIONS, ExportFormat.CSV,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
    }

    @Test
    @SuppressWarnings("unchecked")
    void readyRecordsMetadataOnly() {
        ReportExport export = export();
        export.markReady(UUID.randomUUID(), "extrato.csv", 17);

        auditor.record(ReportExportAuditor.Event.READY, export);

        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(recorder).record(eq(COMPANY), eq(AuditAction.EXPORT), eq(AuditModule.REPORTS),
                eq("report_export"), eq(export.id().toString()), eq("Exportação de relatório: READY"),
                eq(USER), values.capture());
        Map<String, Object> map = values.getValue();
        assertThat(map).containsEntry("event", "READY").containsEntry("type", "TRANSACTIONS")
                .containsEntry("format", "CSV").containsEntry("rowCount", 17)
                .containsEntry("periodFrom", "2026-06-01").containsKey("durationMs");
        assertThat(map.keySet()).containsOnly("event", "exportId", "type", "format", "periodFrom", "periodTo",
                "status", "rowCount", "durationMs");
    }

    @Test
    void deniedUsesExplicitActor() {
        ReportExport export = export();
        UUID other = UUID.randomUUID();

        auditor.record(ReportExportAuditor.Event.ACCESS_DENIED, export, other);

        verify(recorder).record(eq(COMPANY), eq(AuditAction.EXPORT), eq(AuditModule.REPORTS), any(), any(),
                eq("Exportação de relatório: ACCESS_DENIED"), eq(other), any());
    }

    @Test
    void recorderFailureDoesNotPropagate() {
        doThrow(new IllegalStateException("db")).when(recorder)
                .record(any(), any(), any(), any(), any(), any(), any(), any());

        assertThatCode(() -> auditor.record(ReportExportAuditor.Event.REQUESTED, export()))
                .doesNotThrowAnyException();
    }
}
