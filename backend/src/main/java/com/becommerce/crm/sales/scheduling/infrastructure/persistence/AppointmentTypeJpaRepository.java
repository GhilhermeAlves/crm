package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentTypeJpaRepository extends JpaRepository<AppointmentTypeJpaEntity, UUID> {

    List<AppointmentTypeJpaEntity> findByCompanyId(UUID companyId);

    Optional<AppointmentTypeJpaEntity> findByCompanyIdAndSlug(UUID companyId, String slug);
}
