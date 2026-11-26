package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.clinical.document.application.port.out.ReportPdfSource;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentRepository;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Na aba Documentos, o relatório de tricologia abre o PDF publicado (com fotos e código). */
@Component
public class TrichologyReportPdfSource implements ReportPdfSource {

    private final AssessmentRepository assessments;
    private final StorageUseCase storage;

    public TrichologyReportPdfSource(AssessmentRepository assessments, StorageUseCase storage) {
        this.assessments = assessments;
        this.storage = storage;
    }

    @Override
    public byte[] pdfForDocument(UUID companyId, UUID contactId, UUID documentId) {
        TenantContext.setCompanyId(companyId);
        UUID storageId = assessments.findByPatientDocument(companyId, documentId)
                .filter(a -> a.getContactId().equals(contactId))
                .map(TrichologyAssessment::state)
                .map(TrichologyAssessment.State::pdfStorageObjectId)
                .orElse(null);
        return storageId == null ? null : storage.download(companyId, storageId).data();
    }
}
