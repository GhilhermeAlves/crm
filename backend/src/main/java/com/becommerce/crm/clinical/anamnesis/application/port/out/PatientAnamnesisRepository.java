package com.becommerce.crm.clinical.anamnesis.application.port.out;

import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesis;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesisStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientAnamnesisRepository {

    PatientAnamnesis save(PatientAnamnesis anamnesis);

    Optional<PatientAnamnesis> findById(UUID id);

    List<PatientAnamnesis> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId);

    Optional<PatientAnamnesis> findFirstByCompanyIdAndContactIdAndStatusOrderByCompletedAtDesc(
            UUID companyId, UUID contactId, PatientAnamnesisStatus status);

    void delete(PatientAnamnesis anamnesis);
}
