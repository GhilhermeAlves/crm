package com.becommerce.crm.clinical.document.application.port.out;

import com.becommerce.crm.clinical.document.domain.PatientDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientDocumentRepository {

    PatientDocument save(PatientDocument document);

    Optional<PatientDocument> findById(UUID id);

    List<PatientDocument> findByCompanyIdAndContactIdOrderByIssuedAtDesc(UUID companyId, UUID contactId);

    void delete(PatientDocument document);
}
