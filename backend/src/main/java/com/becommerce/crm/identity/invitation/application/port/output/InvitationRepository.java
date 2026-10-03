package com.becommerce.crm.identity.invitation.application.port.output;

import com.becommerce.crm.identity.invitation.domain.Invitation;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Acesso a dados de convites (Sprint 8.5). */
public interface InvitationRepository {

    Invitation save(Invitation invitation);

    Optional<Invitation> findById(UUID id);

    Optional<Invitation> findByTokenHash(String tokenHash);

    List<Invitation> findByCompanyId(UUID companyId, InvitationStatus status);
}