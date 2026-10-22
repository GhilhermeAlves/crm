package com.becommerce.crm.clinical.anamnesis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientAnamnesisJpaRepository extends JpaRepository<PatientAnamnesisJpaEntity, UUID> {

    List<PatientAnamnesisJpaEntity> findByCompanyIdAndContactIdOrderByCreatedAtDesc(
            UUID companyId, UUID contactId);

    Optional<PatientAnamnesisJpaEntity> findFirstByCompanyIdAndContactIdAndStatusOrderByCompletedAtDesc(
            UUID companyId, UUID contactId, String status);
}
