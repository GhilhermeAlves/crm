package com.becommerce.crm.clinical.trichoscopy.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataTrichoscopyRepository extends JpaRepository<PatientTrichoscopyImageJpaEntity, UUID> {
    List<PatientTrichoscopyImageJpaEntity> findByCompanyIdAndContactIdOrderByTakenAtDesc(UUID companyId,
                                                                                         UUID contactId);

    Optional<PatientTrichoscopyImageJpaEntity> findByIdAndCompanyIdAndContactId(UUID id, UUID companyId,
                                                                               UUID contactId);
}
