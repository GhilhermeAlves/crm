package com.becommerce.crm.clinical.document.application.port.in;

import com.becommerce.crm.clinical.document.application.dto.IssuePatientDocumentRequest;
import com.becommerce.crm.clinical.document.application.dto.PatientDocumentPdf;
import com.becommerce.crm.clinical.document.application.dto.PatientDocumentResponse;
import com.becommerce.crm.clinical.document.application.dto.PreviewPatientDocumentRequest;
import com.becommerce.crm.clinical.document.application.dto.PreviewPatientDocumentResponse;

import java.util.List;
import java.util.UUID;

public interface PatientDocumentUseCase {

    List<PatientDocumentResponse> list(UUID companyId, UUID contactId);

    PreviewPatientDocumentResponse preview(UUID companyId, UUID contactId,
                                            PreviewPatientDocumentRequest request, UUID actorId);

    PatientDocumentResponse issue(UUID companyId, UUID contactId, IssuePatientDocumentRequest request,
                                  UUID issuedBy);

    PatientDocumentPdf pdf(UUID companyId, UUID contactId, UUID documentId);

    void delete(UUID companyId, UUID contactId, UUID documentId, boolean admin, UUID actorId);
}
