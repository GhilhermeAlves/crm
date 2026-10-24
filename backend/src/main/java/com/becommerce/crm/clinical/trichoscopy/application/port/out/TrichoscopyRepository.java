package com.becommerce.crm.clinical.trichoscopy.application.port.out;

import com.becommerce.crm.clinical.trichoscopy.domain.PatientTrichoscopyImage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrichoscopyRepository {
    PatientTrichoscopyImage save(PatientTrichoscopyImage image);

    List<PatientTrichoscopyImage> findByCompanyIdAndContactId(UUID companyId, UUID contactId);

    Optional<PatientTrichoscopyImage> findByIdAndCompanyIdAndContactId(UUID id, UUID companyId, UUID contactId);

    void delete(PatientTrichoscopyImage image);
}
