package com.becommerce.crm.identity.invitation.application.dto;

/** Convite com link recém-gerado (reenviar / copiar link). O link anterior deixa de valer. */
public record InvitationLinkResponse(
        InvitationResponse invitation,
        String url
) {}
