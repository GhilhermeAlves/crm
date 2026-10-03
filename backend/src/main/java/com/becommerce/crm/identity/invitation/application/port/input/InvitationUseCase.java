package com.becommerce.crm.identity.invitation.application.port.input;

import com.becommerce.crm.identity.invitation.application.dto.CreateInvitationRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationResponse;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;

import java.util.List;
import java.util.UUID;

/** Casos de uso de convites (Sprint 8.5). */
public interface InvitationUseCase {

    InvitationResponse create(UUID companyId, CreateInvitationRequest request, UUID invitedBy);

    List<InvitationResponse> listByCompany(UUID companyId, InvitationStatus status);

    void revoke(UUID invitationId, UUID companyId);

    InvitationResponse accept(String token, UUID userId);

    InvitationResponse decline(String token, UUID userId);
}