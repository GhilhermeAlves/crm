package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityOverrideJpaRepository extends JpaRepository<AvailabilityOverrideJpaEntity, UUID> {

    List<AvailabilityOverrideJpaEntity> findByCompanyIdAndUserId(UUID companyId, UUID userId);

    @Query("SELECT o FROM AvailabilityOverrideJpaEntity o WHERE o.companyId = :companyId " +
           "AND o.userId = :userId AND o.date >= :from AND o.date <= :to")
    List<AvailabilityOverrideJpaEntity> findByUserIdAndDateRange(@Param("companyId") UUID companyId,
                                                                 @Param("userId") UUID userId,
                                                                 @Param("from") LocalDate from,
                                                                 @Param("to") LocalDate to);

    void deleteByCompanyIdAndUserId(UUID companyId, UUID userId);
}
