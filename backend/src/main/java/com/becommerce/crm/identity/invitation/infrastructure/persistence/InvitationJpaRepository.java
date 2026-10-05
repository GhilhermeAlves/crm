package com.becommerce.crm.identity.invitation.infrastructure.persistence;

import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationJpaRepository extends JpaRepository<JpaInvitation, UUID> {

    Optional<JpaInvitation> findByTokenHash(String tokenHash);

    /** Lock da linha (SELECT ... FOR NO KEY UPDATE): serializa aceites concorrentes do mesmo convite. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from JpaInvitation i where i.tokenHash = :tokenHash")
    Optional<JpaInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<JpaInvitation> findByCompanyIdAndStatus(UUID companyId, InvitationStatus status);

    List<JpaInvitation> findByCompanyId(UUID companyId);
}