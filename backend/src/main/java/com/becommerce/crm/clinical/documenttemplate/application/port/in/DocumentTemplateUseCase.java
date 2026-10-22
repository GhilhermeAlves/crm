package com.becommerce.crm.clinical.documenttemplate.application.port.in;

import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateRequest;
import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateResponse;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;

import java.util.List;
import java.util.UUID;

public interface DocumentTemplateUseCase {

    List<DocumentTemplateResponse> list(UUID companyId, DocumentType type, boolean activeOnly);

    DocumentTemplateResponse get(UUID companyId, UUID templateId);

    DocumentTemplateResponse create(UUID companyId, DocumentTemplateRequest request, UUID createdBy);

    DocumentTemplateResponse update(UUID companyId, UUID templateId, DocumentTemplateRequest request,
                                    UUID createdBy);

    void delete(UUID companyId, UUID templateId, UUID createdBy);
}
