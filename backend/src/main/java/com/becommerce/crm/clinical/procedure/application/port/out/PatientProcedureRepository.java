package com.becommerce.crm.clinical.procedure.application.port.out;

import com.becommerce.crm.clinical.procedure.domain.PatientProcedure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientProcedureRepository {

    PatientProcedure save(PatientProcedure procedure);

    Optional<PatientProcedure> findById(UUID id);

    List<PatientProcedure> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId);

    void delete(PatientProcedure procedure);
}
