package com.becommerce.crm.infrastructure.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    List<NotificationJpaEntity> findByCompanyIdAndUserIdOrderByCreatedAtDesc(UUID companyId, UUID userId);

    long countByCompanyIdAndUserIdAndReadAtIsNull(UUID companyId, UUID userId);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.readAt = CURRENT_TIMESTAMP "
            + "WHERE n.companyId = :companyId AND n.userId = :userId AND n.readAt IS NULL")
    void markAllRead(@Param("companyId") UUID companyId, @Param("userId") UUID userId);
}