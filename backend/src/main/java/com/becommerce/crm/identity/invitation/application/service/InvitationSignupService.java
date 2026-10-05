package com.becommerce.crm.identity.invitation.application.service;

import com.becommerce.crm.identity.application.port.output.PasswordEncoder;
import com.becommerce.crm.identity.application.service.KeycloakSignupSaga;
import com.becommerce.crm.identity.domain.valueobject.Password;
import com.becommerce.crm.identity.invitation.application.dto.InvitationPreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationRegisterRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationResponse;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.identity.invitation.infrastructure.rate.InvitationRateLimiter;
import org.springframework.stereotype.Service;

/**
 * Endpoints públicos do convite (sem login): prévia e cadastro de quem ainda
 * não tem conta.
 *
 * <p>Cadastro: pré-valida o convite (sem tocar no Keycloak), cria o usuário no
 * Keycloak com o e-mail DO CONVITE e grava o restante numa transação via
 * {@link KeycloakSignupSaga}, que desfaz o Keycloak quando o PostgreSQL não
 * gravou. Não é transacional de propósito.
 */
@Service
public class InvitationSignupService {

    private final InvitationService invitationService;
    private final KeycloakSignupSaga signupSaga;
    private final InvitationRateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;

    public InvitationSignupService(InvitationService invitationService,
                                   KeycloakSignupSaga signupSaga,
                                   InvitationRateLimiter rateLimiter,
                                   PasswordEncoder passwordEncoder) {
        this.invitationService = invitationService;
        this.signupSaga = signupSaga;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
    }

    public InvitationPreviewResponse preview(String token, String clientKey) {
        checkRate(clientKey);
        return invitationService.preview(token);
    }

    public InvitationResponse register(InvitationRegisterRequest request, String clientKey) {
        checkRate(clientKey);
        InvitationResponse invitation = invitationService.prepareSignup(request.token());
        String encodedPassword = passwordEncoder.encode(new Password(request.password()).value());
        String name = request.name().trim();

        return signupSaga.execute(invitation.email(), request.password(), name,
                keycloakUserId -> invitationService.completeSignup(
                        request.token(), keycloakUserId, encodedPassword, name),
                () -> accepted(invitation));
    }

    private void checkRate(String clientKey) {
        if (!rateLimiter.tryPublic(clientKey)) {
            throw new IllegalStateException("Muitas tentativas. Tente novamente mais tarde.");
        }
    }

    private static InvitationResponse accepted(InvitationResponse pending) {
        return new InvitationResponse(pending.id(), pending.companyId(), pending.email(), pending.inviteeName(),
                pending.role(), InvitationStatus.ACCEPTED, pending.invitedBy(), pending.expiresAt(),
                pending.createdAt());
    }
}
