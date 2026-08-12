package com.becommerce.crm.application.invitation.port.output;

import com.becommerce.crm.domain.invitation.Invitation;
import com.becommerce.crm.domain.invitation.InvitationStatus;

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