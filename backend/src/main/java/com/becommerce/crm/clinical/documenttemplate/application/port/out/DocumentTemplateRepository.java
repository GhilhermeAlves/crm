package com.becommerce.crm.clinical.documenttemplate.application.port.out;

import com.becommerce.crm.clinical.documenttemplate.domain.DocumentTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentTemplateRepository {

    DocumentTemplate save(DocumentTemplate template);

    Optional<DocumentTemplate> findById(UUID id);

    List<DocumentTemplate> findByCompanyIdOrderByTypeAscTitleAsc(UUID companyId);

    long countByCompanyId(UUID companyId);

    void delete(DocumentTemplate template);
}
