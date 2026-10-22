package com.becommerce.crm.clinical.evolution.application.port.out;

import com.becommerce.crm.clinical.evolution.domain.PatientEvolution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientEvolutionRepository {

    PatientEvolution save(PatientEvolution evolution);

    Optional<PatientEvolution> findById(UUID id);

    List<PatientEvolution> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId);

    void delete(PatientEvolution evolution);
}
