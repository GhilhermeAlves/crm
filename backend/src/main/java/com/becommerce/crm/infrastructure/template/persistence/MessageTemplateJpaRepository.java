package com.becommerce.crm.infrastructure.template.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageTemplateJpaRepository extends JpaRepository<MessageTemplateJpaEntity, UUID> {

    @Query("SELECT t FROM MessageTemplateJpaEntity t WHERE t.companyId = :companyId " +
            "AND (:channelType IS NULL OR :channelType = '' OR t.channelType = :channelType) " +
            "AND (:status IS NULL OR :status = '' OR t.status = :status)")
    Page<MessageTemplateJpaEntity> findByCompanyWithFilters(
            @Param("companyId") UUID companyId,
            @Param("channelType") String channelType,
            @Param("status") String status,
            Pageable pageable);
}
