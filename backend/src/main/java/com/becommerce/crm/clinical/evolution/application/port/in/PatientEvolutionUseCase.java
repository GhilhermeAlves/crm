package com.becommerce.crm.clinical.evolution.application.port.in;

import com.becommerce.crm.clinical.evolution.application.dto.PatientEvolutionRequest;
import com.becommerce.crm.clinical.evolution.application.dto.PatientEvolutionResponse;

import java.util.List;
import java.util.UUID;

public interface PatientEvolutionUseCase {

    List<PatientEvolutionResponse> list(UUID companyId, UUID contactId);

    PatientEvolutionResponse create(UUID companyId, UUID contactId, PatientEvolutionRequest request,
                                    UUID createdBy);

    PatientEvolutionResponse update(UUID companyId, UUID contactId, UUID evolutionId,
                                    PatientEvolutionRequest request, UUID actorId, boolean admin);

    PatientEvolutionResponse finalizeEvolution(UUID companyId, UUID contactId, UUID evolutionId, UUID actorId);

    void delete(UUID companyId, UUID contactId, UUID evolutionId, UUID actorId, boolean admin);
}
