package com.becommerce.crm.identity.invitation.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyInvitationJpaRepository extends JpaRepository<JpaCompanyInvitation, UUID> {

    Optional<JpaCompanyInvitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from JpaCompanyInvitation i where i.tokenHash = :tokenHash")
    Optional<JpaCompanyInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<JpaCompanyInvitation> findByInviterCompanyIdOrderByCreatedAtDesc(UUID inviterCompanyId);
}
