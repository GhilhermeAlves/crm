package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExportGeneratorTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    private ReportExportRepository repository;
    private ReportDataGateway gateway;
    private StorageUseCase storage;
    private NotificationUseCase notifications;
    private ReportExportGenerator generator;

    @BeforeEach
    void setUp() {
        repository = mock(ReportExportRepository.class);
        gateway = mock(ReportDataGateway.class);
        storage = mock(StorageUseCase.class);
        notifications = mock(NotificationUseCase.class);
        when(gateway.companyHeader(COMPANY)).thenReturn(new CompanyHeader("Clínica", "00.000.000/0001-00"));
        generator = new ReportExportGenerator(repository, gateway, new ReportDatasetBuilder(gateway), storage,
                notifications, new ObjectMapper());
    }

    private ReportExport pending(ReportExportType type, ExportFormat format) {
        ReportExport export = ReportExport.pending(COMPANY, USER, type, format, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30));
        when(repository.findById(COMPANY, export.id())).thenReturn(Optional.of(export));
        return export;
    }

    @Test
    void successUploadsMarksReadyAndNotifiesWithDownloadMetadata() throws Exception {
        ReportExport export = pending(ReportExportType.MONTHLY_DRE, ExportFormat.PDF);
        UUID objectId = UUID.randomUUID();
        when(storage.upload(eq(COMPANY), anyString(), anyString(), any(), eq(USER)))
                .thenReturn(new StorageResponse(objectId, "k", "f", "application/pdf", 10, COMPANY,
                        LocalDateTime.now(), null));

        generator.generate(COMPANY, export.id());

        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(storage).upload(eq(COMPANY), eq("contabil-mensal_2026-06.pdf"), eq("application/pdf"),
                bytes.capture(), eq(USER));
        assertThat(new String(bytes.getValue(), 0, 4)).isEqualTo("%PDF");
        assertThat(export.status()).isEqualTo(ReportExportStatus.READY);
        assertThat(export.storageObjectId()).isEqualTo(objectId);
        verify(repository).update(export);

        ArgumentCaptor<CreateNotificationRequest> notification =
                ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notifications).create(eq(COMPANY), notification.capture(), eq(USER));
        assertThat(notification.getValue().userId()).isEqualTo(USER);
        assertThat(notification.getValue().body()).isEqualTo("Contábil mensal (DRE) · Junho de 2026 · PDF");
        JsonNode metadata = new ObjectMapper().readTree(notification.getValue().metadata());
        assertThat(metadata.get("kind").asText()).isEqualTo("REPORT_EXPORT");
        assertThat(metadata.get("exportId").asText()).isEqualTo(export.id().toString());
        assertThat(metadata.get("fileName").asText()).isEqualTo("contabil-mensal_2026-06.pdf");
    }

    @Test
    void failureMarksFailedAndNotifiesWithoutLink() {
        ReportExport export = pending(ReportExportType.AGENDA, ExportFormat.XLSX);
        when(gateway.appointments(any(), any(), any())).thenThrow(new IllegalStateException("db down"));

        generator.generate(COMPANY, export.id());

        assertThat(export.status()).isEqualTo(ReportExportStatus.FAILED);
        verify(repository).update(export);
        verify(storage, never()).upload(any(), any(), any(), any(), any());
        ArgumentCaptor<CreateNotificationRequest> notification =
                ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notifications).create(eq(COMPANY), notification.capture(), eq(USER));
        assertThat(notification.getValue().title()).isEqualTo("Não foi possível gerar o relatório");
        assertThat(notification.getValue().metadata()).isNull();
    }

    @Test
    void xlsxFileNameUsesRange() {
        ReportExport export = pending(ReportExportType.CASH_FLOW, ExportFormat.XLSX);
        when(gateway.paidByDay(any(), any(), any())).thenReturn(List.of());
        when(storage.upload(any(), anyString(), anyString(), any(), any()))
                .thenReturn(new StorageResponse(UUID.randomUUID(), "k", "f", "x", 1, COMPANY, LocalDateTime.now(), null));

        generator.generate(COMPANY, export.id());

        verify(storage).upload(eq(COMPANY), eq("fluxo-de-caixa_2026-06-01_a_2026-06-30.xlsx"),
                eq(ExportFormat.XLSX.contentType()), any(), eq(USER));
    }

    @Test
    void alreadyFinishedExportIsIgnored() {
        ReportExport export = pending(ReportExportType.CASH_FLOW, ExportFormat.PDF);
        export.markFailed("x");

        generator.generate(COMPANY, export.id());

        verify(storage, never()).upload(any(), any(), any(), any(), any());
        verify(notifications, never()).create(any(), any(), any());
    }
}
