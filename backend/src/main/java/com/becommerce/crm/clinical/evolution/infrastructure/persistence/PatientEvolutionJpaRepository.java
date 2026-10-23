package com.becommerce.crm.clinical.evolution.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientEvolutionJpaRepository extends JpaRepository<PatientEvolutionJpaEntity, UUID> {

    List<PatientEvolutionJpaEntity> findByCompanyIdAndContactIdOrderByCreatedAtDesc(
            UUID companyId, UUID contactId);
}
