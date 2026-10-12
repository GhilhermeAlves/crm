package com.becommerce.crm.reports.export.application.dto;

import com.becommerce.crm.reports.export.domain.ReportExport;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Situação de uma exportação para a tela de relatórios e o sino. */
public record ReportExportResponse(
        UUID id,
        String type,
        String typeLabel,
        String format,
        LocalDate periodFrom,
        LocalDate periodTo,
        String status,
        String fileName,
        String errorMessage,
        Instant createdAt,
        Instant finishedAt
) {
    public static ReportExportResponse from(ReportExport export) {
        return new ReportExportResponse(export.id(), export.type().name(), export.type().label(),
                export.format().name(), export.periodFrom(), export.periodTo(), export.status().name(),
                export.fileName(), export.errorMessage(), export.createdAt(), export.finishedAt());
    }
}
