package com.becommerce.crm.reports.export.domain.exception;

import java.util.UUID;

/** Exportação inexistente, de outro usuário/empresa ou ainda não disponível (404). */
public class ReportExportNotFoundException extends RuntimeException {

    public ReportExportNotFoundException(UUID id) {
        super("Exportação de relatório não encontrada: " + id);
    }
}
