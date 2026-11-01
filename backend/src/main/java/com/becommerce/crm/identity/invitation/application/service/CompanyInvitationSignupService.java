package com.becommerce.crm.identity.invitation.application.service;

import com.becommerce.crm.identity.application.port.output.PasswordEncoder;
import com.becommerce.crm.identity.application.service.KeycloakSignupSaga;
import com.becommerce.crm.identity.domain.valueobject.Password;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.PreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.RegisterRequest;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.Response;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.identity.invitation.infrastructure.rate.InvitationRateLimiter;
import org.springframework.stereotype.Service;

/**
 * Endpoints públicos do convite de empresa: prévia e cadastro de quem ainda não
 * tem conta. Mesmo desenho do {@link InvitationSignupService}: pré-valida,
 * cria o usuário no Keycloak e grava o resto numa transação via
 * {@link KeycloakSignupSaga}, que desfaz o Keycloak se o PostgreSQL falhar.
 */
@Service
public class CompanyInvitationSignupService {

    private final CompanyInvitationService invitationService;
    private final KeycloakSignupSaga signupSaga;
    private final InvitationRateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;

    public CompanyInvitationSignupService(CompanyInvitationService invitationService,
                                          KeycloakSignupSaga signupSaga,
                                          InvitationRateLimiter rateLimiter,
                                          PasswordEncoder passwordEncoder) {
        this.invitationService = invitationService;
        this.signupSaga = signupSaga;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
    }

    public PreviewResponse preview(String token, String clientKey) {
        checkRate(clientKey);
        return invitationService.preview(token);
    }

    public Response register(RegisterRequest request, String clientKey) {
        checkRate(clientKey);
        Response invitation = invitationService.prepareSignup(request.token());
        String encodedPassword = passwordEncoder.encode(new Password(request.password()).value());
        String name = request.name().trim();

        return signupSaga.execute(invitation.email(), request.password(), name,
                keycloakUserId -> invitationService.completeSignup(
                        request.token(), keycloakUserId, encodedPassword, name, request.company()),
                () -> accepted(invitation));
    }

    /** O commit é atômico: se o usuário existe, a empresa e o aceite também foram gravados. */
    private static Response accepted(Response pending) {
        return new Response(pending.id(), pending.email(), pending.inviteeName(), pending.segment(),
                pending.enabledModules(), pending.plan(), pending.maxUsers(), InvitationStatus.ACCEPTED,
                pending.companyId(), pending.invitedBy(), pending.expiresAt(), pending.createdAt());
    }

    private void checkRate(String clientKey) {
        if (!rateLimiter.tryPublic(clientKey)) {
            throw new IllegalStateException("Muitas tentativas. Tente novamente mais tarde.");
        }
    }
}
