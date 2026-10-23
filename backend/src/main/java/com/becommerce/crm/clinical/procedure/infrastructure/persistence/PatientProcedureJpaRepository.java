package com.becommerce.crm.clinical.procedure.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientProcedureJpaRepository extends JpaRepository<PatientProcedureJpaEntity, UUID> {

    List<PatientProcedureJpaEntity> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId);
}
