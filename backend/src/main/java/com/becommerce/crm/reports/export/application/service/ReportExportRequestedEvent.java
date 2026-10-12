package com.becommerce.crm.reports.export.application.service;

import java.util.UUID;

/** Publicado ao gravar um pedido PENDING; dispara a geração após o commit. */
public record ReportExportRequestedEvent(UUID companyId, UUID exportId) {
}
