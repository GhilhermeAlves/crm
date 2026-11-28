package com.becommerce.crm.clinical.document.application.port.out;

import java.util.UUID;

/**
 * PDF já gerado e guardado por outro módulo para um documento emitido (ex.:
 * relatório de tricologia, com fotos e código de verificação). Devolve
 * {@code null} quando o documento não pertence ao módulo.
 */
public interface ReportPdfSource {
    byte[] pdfForDocument(UUID companyId, UUID contactId, UUID documentId);
}
