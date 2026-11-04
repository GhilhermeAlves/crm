package com.becommerce.crm.identity.invitation.infrastructure.persistence;

import com.becommerce.crm.identity.invitation.application.service.InvitationTokenService;
import com.becommerce.crm.identity.invitation.domain.CompanyInvitation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyInvitationRepositoryImplTest {

    /**
     * O aceite precisa chegar ao banco enquanto o token do convite está no
     * contexto RLS (limpo antes do commit). Regressão do 500 em
     * /company-invitations/register (StaleObjectStateException).
     */
    @Test
    void saveFlushesImmediately() {
        CompanyInvitationJpaRepository jpa = mock(CompanyInvitationJpaRepository.class);
        when(jpa.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        CompanyInvitation invitation = CompanyInvitation.create(UUID.randomUUID(), "dono@clinica.com", "Dono",
                "DENTAL", "CRM", "PROFESSIONAL", 3, null, InvitationTokenService.hash("tok"), UUID.randomUUID());

        new CompanyInvitationRepositoryImpl(jpa).save(invitation);

        verify(jpa).saveAndFlush(any());
        verify(jpa, never()).save(any());
    }
}
