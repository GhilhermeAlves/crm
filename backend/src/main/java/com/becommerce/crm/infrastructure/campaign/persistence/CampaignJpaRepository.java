package com.becommerce.crm.infrastructure.campaign.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignJpaRepository extends JpaRepository<CampaignJpaEntity, UUID> {

    @Query("SELECT c FROM CampaignJpaEntity c WHERE c.companyId = :companyId " +
            "AND (:status IS NULL OR :status = '' OR c.status = :status) " +
            "AND (:audienceType IS NULL OR :audienceType = '' OR c.audienceType = :audienceType)")
    Page<CampaignJpaEntity> findByCompanyWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("audienceType") String audienceType,
            Pageable pageable);

    /** Claim atômico SCHEDULED -> RUNNING (idempotência do start). */
    @Modifying
    @Query(value = "UPDATE campaigns SET status = 'RUNNING', started_at = COALESCE(started_at, CURRENT_TIMESTAMP), " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = :id AND status = 'SCHEDULED'", nativeQuery = true)
    int claimForExecution(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE campaigns SET status = 'SCHEDULED', updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :id AND status = 'RUNNING'", nativeQuery = true)
    int resetToScheduled(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE campaigns SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = :id AND status = 'RUNNING'", nativeQuery = true)
    int completeIfRunning(@Param("id") UUID id);
}
