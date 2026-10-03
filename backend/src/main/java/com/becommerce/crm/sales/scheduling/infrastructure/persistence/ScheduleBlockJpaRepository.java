package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduleBlockJpaRepository extends JpaRepository<ScheduleBlockJpaEntity, UUID> {

    @Query("SELECT b FROM ScheduleBlockJpaEntity b WHERE b.companyId = :companyId " +
           "AND b.hostId = :hostId AND b.startAt < :to AND b.endAt > :from")
    List<ScheduleBlockJpaEntity> findByHostIdAndRange(@Param("companyId") UUID companyId,
                                                      @Param("hostId") UUID hostId,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to);

    @Query("SELECT b FROM ScheduleBlockJpaEntity b WHERE b.companyId = :companyId " +
           "AND b.startAt < :to AND b.endAt > :from " +
           "AND (:hostIds IS NULL OR b.hostId IN :hostIds)")
    List<ScheduleBlockJpaEntity> findByCompanyIdAndRange(@Param("companyId") UUID companyId,
                                                         @Param("from") Instant from,
                                                         @Param("to") Instant to,
                                                         @Param("hostIds") List<UUID> hostIds);
}
