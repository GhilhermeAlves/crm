package com.becommerce.crm.sales.tag.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagJpaRepository extends JpaRepository<TagJpaEntity, UUID> {

    List<TagJpaEntity> findByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<TagJpaEntity> findByCompanyIdAndName(UUID companyId, String name);
}
