package com.becommerce.crm.clinical.document.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientDocumentJpaRepository extends JpaRepository<PatientDocumentJpaEntity, UUID> {

    List<PatientDocumentJpaEntity> findByCompanyIdAndContactIdOrderByIssuedAtDesc(
            UUID companyId, UUID contactId);
}
