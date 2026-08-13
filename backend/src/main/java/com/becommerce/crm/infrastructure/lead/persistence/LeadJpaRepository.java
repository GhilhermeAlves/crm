package com.becommerce.crm.infrastructure.lead.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LeadJpaRepository extends JpaRepository<LeadJpaEntity, UUID> {

    boolean existsByContactIdAndCompanyId(UUID contactId, UUID companyId);

    @Query("SELECT l FROM LeadJpaEntity l WHERE l.companyId = :companyId " +
            "AND (:status IS NULL OR :status = '' OR l.status = :status) " +
            "AND (:source IS NULL OR :source = '' OR l.source = :source) " +
            "AND (:classification IS NULL OR :classification = '' OR l.classification = :classification)")
    Page<LeadJpaEntity> findByCompanyWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("source") String source,
            @Param("classification") String classification,
            Pageable pageable);
}