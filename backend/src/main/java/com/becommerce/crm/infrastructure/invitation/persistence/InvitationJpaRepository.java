package com.becommerce.crm.infrastructure.invitation.persistence;

import com.becommerce.crm.domain.invitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationJpaRepository extends JpaRepository<JpaInvitation, UUID> {

    Optional<JpaInvitation> findByTokenHash(String tokenHash);

    List<JpaInvitation> findByCompanyIdAndStatus(UUID companyId, InvitationStatus status);

    List<JpaInvitation> findByCompanyId(UUID companyId);
}