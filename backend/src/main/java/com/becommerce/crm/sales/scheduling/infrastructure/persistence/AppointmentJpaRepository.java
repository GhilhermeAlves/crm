package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    @Query("SELECT a FROM AppointmentJpaEntity a WHERE a.companyId = :companyId " +
           "AND a.startAt < :to AND a.endAt > :from " +
           "AND (:hostIds IS NULL OR a.hostId IN :hostIds)")
    List<AppointmentJpaEntity> findByCompanyIdAndRange(@Param("companyId") UUID companyId,
                                                       @Param("from") Instant from,
                                                       @Param("to") Instant to,
                                                       @Param("hostIds") List<UUID> hostIds);

    @Query("SELECT a FROM AppointmentJpaEntity a WHERE a.hostId = :hostId " +
           "AND a.status <> 'CANCELED' " +
           "AND a.startAt < :to AND a.endAt > :from")
    List<AppointmentJpaEntity> findNonCanceledByHostAndRange(@Param("hostId") UUID hostId,
                                                             @Param("from") Instant from,
                                                             @Param("to") Instant to);

    Optional<AppointmentJpaEntity> findByPublicToken(String publicToken);

    @Query("SELECT a FROM AppointmentJpaEntity a WHERE a.companyId = :companyId " +
           "AND a.contactId = :contactId AND a.status <> 'CANCELED' " +
           "AND a.startAt >= :from ORDER BY a.startAt ASC")
    List<AppointmentJpaEntity> findUpcomingByContact(@Param("companyId") UUID companyId,
                                                     @Param("contactId") UUID contactId,
                                                     @Param("from") Instant from,
                                                     org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(a) FROM AppointmentJpaEntity a WHERE a.hostId = :hostId " +
           "AND a.status <> 'CANCELED' " +
           "AND a.startAt >= :weekStart AND a.startAt < :weekEnd")
    long countByHostIdInWeek(@Param("hostId") UUID hostId,
                             @Param("weekStart") Instant weekStart,
                             @Param("weekEnd") Instant weekEnd);
}
