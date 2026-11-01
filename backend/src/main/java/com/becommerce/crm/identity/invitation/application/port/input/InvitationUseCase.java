package com.becommerce.crm.identity.invitation.application.port.input;

import com.becommerce.crm.identity.invitation.application.dto.CreateInvitationRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationLinkResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationPreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationResponse;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;

import java.util.List;
import java.util.UUID;

/** Casos de uso de convites (Sprint 8.5). */
public interface InvitationUseCase {

    /** {@code canInviteAdmin}: só SUPER_ADMIN/CRM_ADMIN convidam o papel ADMIN. */
    InvitationLinkResponse create(UUID companyId, CreateInvitationRequest request, UUID invitedBy,
                                  boolean canInviteAdmin);

    List<InvitationResponse> listByCompany(UUID companyId, InvitationStatus status);

    void revoke(UUID invitationId, UUID companyId);

    /** Novo token e nova validade (o link anterior deixa de valer); envia o e-mail se pedido. */
    InvitationLinkResponse regenerate(UUID companyId, UUID invitationId, boolean sendEmail, UUID requestedBy);

    /** Prévia pública do convite (empresa, e-mail, perfil, status, se o e-mail já tem conta). */
    InvitationPreviewResponse preview(String token);

    InvitationResponse accept(String token, UUID userId);

    InvitationResponse decline(String token, UUID userId);
}
