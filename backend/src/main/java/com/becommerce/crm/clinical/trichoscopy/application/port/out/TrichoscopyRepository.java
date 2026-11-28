package com.becommerce.crm.clinical.trichoscopy.application.port.out;

import com.becommerce.crm.clinical.trichoscopy.domain.PatientTrichoscopyImage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrichoscopyRepository {
    PatientTrichoscopyImage save(PatientTrichoscopyImage image);

    /** Todas as fotos, inclusive as ocultas (usado pelos exames de tricologia). */
    List<PatientTrichoscopyImage> findByCompanyIdAndContactId(UUID companyId, UUID contactId);

    /** Só as fotos visíveis na galeria. */
    List<PatientTrichoscopyImage> findVisibleByCompanyIdAndContactId(UUID companyId, UUID contactId);

    Optional<PatientTrichoscopyImage> findByIdAndCompanyIdAndContactId(UUID id, UUID companyId, UUID contactId);

    void delete(PatientTrichoscopyImage image);
}
