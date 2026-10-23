package com.becommerce.crm.clinical.anamnesis.application.port.in;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisResponse;
import com.becommerce.crm.clinical.anamnesis.application.dto.SavePatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.dto.StartPatientAnamnesisRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientAnamnesisUseCase {

    List<PatientAnamnesisResponse> list(UUID companyId, UUID contactId);

    Optional<PatientAnamnesisResponse> current(UUID companyId, UUID contactId);

    PatientAnamnesisResponse get(UUID companyId, UUID contactId, UUID anamnesisId);

    PatientAnamnesisResponse start(UUID companyId, UUID contactId, StartPatientAnamnesisRequest request,
                                   UUID createdBy);

    PatientAnamnesisResponse save(UUID companyId, UUID contactId, UUID anamnesisId,
                                  SavePatientAnamnesisRequest request, UUID actorId, boolean admin);

    void delete(UUID companyId, UUID contactId, UUID anamnesisId, UUID actorId, boolean admin);
}
