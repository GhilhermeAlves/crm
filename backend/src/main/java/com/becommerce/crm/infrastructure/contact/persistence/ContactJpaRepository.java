package com.becommerce.crm.infrastructure.contact.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactJpaRepository extends JpaRepository<ContactJpaEntity, UUID> {

    long countByCompanyIdAndDeletedAtIsNull(UUID companyId);

    List<ContactJpaEntity> findByCompanyIdAndDeletedAtIsNullOrderByFirstNameAscLastNameAsc(UUID companyId);
}