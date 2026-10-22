package com.becommerce.crm.clinical.documenttemplate.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentTemplateJpaRepository extends JpaRepository<DocumentTemplateJpaEntity, UUID> {

    List<DocumentTemplateJpaEntity> findByCompanyIdOrderByTypeAscTitleAsc(UUID companyId);

    long countByCompanyId(UUID companyId);
}
