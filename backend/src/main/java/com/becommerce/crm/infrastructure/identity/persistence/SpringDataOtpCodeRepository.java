package com.becommerce.crm.infrastructure.identity.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataOtpCodeRepository extends JpaRepository<OtpCodeJpaEntity, UUID> {
    
    @Query("SELECT o FROM OtpCodeJpaEntity o WHERE o.phoneE164 = :phone ORDER BY o.createdAt DESC")
    List<OtpCodeJpaEntity> findLatestByPhone(@Param("phone") String phoneE164, Pageable pageable);
    
    @Query("DELETE FROM OtpCodeJpaEntity o WHERE o.expiresAt < :now")
    void deleteExpired(@Param("now") java.time.LocalDateTime now);
}