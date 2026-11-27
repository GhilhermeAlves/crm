package com.becommerce.crm.reports.export.application.dto;

import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExportType;

import java.util.List;

/** Item do {@code GET /report-exports/catalog}: metadados de um relatório que o usuário pode gerar. */
public record ReportCatalogEntry(String type, String slug, String label, String description, String domain,
                                 String permission, List<String> formats, String periodMode, int maxMonths) {

    public static ReportCatalogEntry from(ReportExportType type) {
        return new ReportCatalogEntry(type.name(), type.slug(), type.label(), type.description(),
                type.domain().name(), type.requiredPermission(),
                type.formats().stream().map(ExportFormat::name).toList(),
                type.periodMode().name(), type.maxMonths());
    }
}
