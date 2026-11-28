package com.becommerce.crm.clinical.anamnesis.divergence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileDivergenceJpaRepository extends JpaRepository<ProfileDivergenceJpaEntity, UUID> {

    List<ProfileDivergenceJpaEntity> findByCompanyIdAndPersonIdAndStatusOrderByCreatedAtDesc(
            UUID companyId, UUID personId, String status);

    boolean existsByCompanyIdAndPersonIdAndFieldAndProposedValueAndStatus(
            UUID companyId, UUID personId, String field, String proposedValue, String status);

    Optional<ProfileDivergenceJpaEntity> findByIdAndCompanyIdAndPersonId(UUID id, UUID companyId, UUID personId);
}
