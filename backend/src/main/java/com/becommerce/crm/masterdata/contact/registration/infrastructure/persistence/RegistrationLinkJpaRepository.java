package com.becommerce.crm.masterdata.contact.registration.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationLinkJpaRepository extends JpaRepository<JpaRegistrationLink, UUID> {

    Optional<JpaRegistrationLink> findByToken(String token);

    /** Lock da linha: serializa submits concorrentes do mesmo link. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from JpaRegistrationLink l where l.token = :token")
    Optional<JpaRegistrationLink> findByTokenForUpdate(@Param("token") String token);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update JpaRegistrationLink l set l.usedAt = :now "
            + "where l.companyId = :companyId and l.contactId = :contactId and l.usedAt is null")
    int invalidateUnused(@Param("companyId") UUID companyId, @Param("contactId") UUID contactId,
                         @Param("now") LocalDateTime now);

    /** Atualiza só used_at (a policy de token libera UPDATE apenas da linha do token). */
    @Modifying(flushAutomatically = true)
    @Query("update JpaRegistrationLink l set l.usedAt = :now where l.id = :id")
    int markUsed(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
