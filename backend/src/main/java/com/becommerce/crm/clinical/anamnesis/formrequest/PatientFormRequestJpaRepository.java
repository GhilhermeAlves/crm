package com.becommerce.crm.clinical.anamnesis.formrequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientFormRequestJpaRepository extends JpaRepository<PatientFormRequestJpaEntity, UUID> {

    List<PatientFormRequestJpaEntity> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId,
                                                                                      UUID contactId);

    Optional<PatientFormRequestJpaEntity> findByTokenHash(String tokenHash);
}
