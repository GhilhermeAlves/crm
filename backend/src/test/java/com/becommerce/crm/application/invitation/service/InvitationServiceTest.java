package com.becommerce.crm.application.invitation.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.invitation.dto.CreateInvitationRequest;
import com.becommerce.crm.application.invitation.dto.InvitationResponse;
import com.becommerce.crm.application.invitation.port.output.InvitationRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.application.notification.EmailSender;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyPlan;
import com.becommerce.crm.domain.company.CompanyStatus;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.domain.invitation.Invitation;
import com.becommerce.crm.domain.invitation.InvitationStatus;
import com.becommerce.crm.domain.invitation.exception.InvitationNotFoundException;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.infrastructure.invitation.persistence.InvitationTokenContextHolder;
import com.becommerce.crm.infrastructure.invitation.rate.InvitationRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock InvitationRepository invitationRepository;
    @Mock CompanyRepository companyRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock EmailSender emailSender;
    @Mock InvitationTokenContextHolder tokenContext;
    @Mock InvitationRateLimiter rateLimiter;

    @InjectMocks InvitationService invitationService;

    private UUID companyId;
    private UUID invitedBy;
    private Invitation pending;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        invitedBy = UUID.randomUUID();
        // Rate limiter permitido por padrão nos testes de fluxo principal
        // (lenient: testes que não acionam o limiter não devem falhar por stub não usado).
        lenient().when(rateLimiter.tryCreate(anyString())).thenReturn(true);
        lenient().when(rateLimiter.tryAccept(anyString())).thenReturn(true);
    }

    private Company activeCompany() {
        return Company.create(
                "Empresa LTDA", "EmpresaX", "12345678000190",
                "123456789", "987654321",
                "admin@empresa.com", "(11) 99999-0000", null,
                "01001000", "Rua X", "1", null,
                "Centro", "SP", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, 500, null, null);
    }

    @Test
    void shouldCreateInvitation() {
        Company company = activeCompany();
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING)).thenReturn(List.of());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationResponse response = invitationService.create(
                companyId, new CreateInvitationRequest("novo@empresa.com", "AGENT"), invitedBy);

        assertNotNull(response.id());
        assertEquals("novo@empresa.com", response.email());
        assertEquals("AGENT", response.role());
        assertEquals(InvitationStatus.PENDING, response.status());
        assertNotEquals(response.id(), pending == null ? null : pending.getId());

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        // token nunca é armazenado em claro
        assertTrue(captor.getValue().getTokenHash().length() == 64);
        verify(emailSender).sendInvitation(eq("novo@empresa.com"), eq("EmpresaX"), eq("AGENT"), anyString());
    }

    @Test
    void shouldRejectInvalidRole() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany()));
        assertThrows(IllegalArgumentException.class,
                () -> invitationService.create(companyId, new CreateInvitationRequest("a@b.com", "SUPER_ADMIN"), invitedBy));
        assertThrows(IllegalArgumentException.class,
                () -> invitationService.create(companyId, new CreateInvitationRequest("a@b.com", "OWNER"), invitedBy));
    }

    @Test
    void shouldRejectDuplicatePending() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany()));
        Invitation existing = Invitation.create(companyId, "dup@empresa.com", "AGENT", "x".repeat(64), invitedBy);
        when(invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING))
                .thenReturn(List.of(existing));
        assertThrows(IllegalArgumentException.class,
                () -> invitationService.create(companyId, new CreateInvitationRequest("dup@empresa.com", "AGENT"), invitedBy));
    }

    @Test
    void shouldRejectInactiveCompany() {
        Company company = activeCompany();
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());
        assertThrows(com.becommerce.crm.domain.company.CompanyNotFoundException.class,
                () -> invitationService.create(companyId, new CreateInvitationRequest("a@b.com", "AGENT"), invitedBy));
    }

    @Test
    void shouldAcceptInvitation() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(new Email("convite@empresa.com"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepository.existsActiveByUserIdAndCompanyId(userId, companyId)).thenReturn(false);

        pending = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        Role adminRole = mock(Role.class);
        when(adminRole.getId()).thenReturn(UUID.randomUUID());
        when(roleRepository.findByNameAndCompanyId("AGENT", companyId)).thenReturn(Optional.of(adminRole));
        when(userRoleRepository.existsByUserIdAndRoleId(userId, adminRole.getId())).thenReturn(false);

        InvitationResponse response = invitationService.accept("tok-abc", userId);

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        verify(membershipRepository).save(any(Membership.class));
        verify(userRoleRepository).save(any());
        verify(user).grantCrmAccess();
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectAcceptWhenEmailMismatch() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getEmail()).thenReturn(new Email("outra@empresa.com"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        pending = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));

        assertThrows(IllegalArgumentException.class, () -> invitationService.accept("tok-abc", userId));
    }

    @Test
    void shouldRejectAcceptWhenExpired() {
        pending = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        // força expiração
        setExpired(pending);
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));

        assertThrows(IllegalStateException.class, () -> invitationService.accept("tok-abc", UUID.randomUUID()));
        assertEquals(InvitationStatus.EXPIRED, pending.getStatus());
    }

    @Test
    void shouldRejectAcceptForInvalidToken() {
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThrows(InvitationNotFoundException.class, () -> invitationService.accept("unknown", UUID.randomUUID()));
    }

    @Test
    void shouldDeclineByRevoking() {
        pending = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationResponse response = invitationService.decline("tok-abc", UUID.randomUUID());
        assertEquals(InvitationStatus.REVOKED, response.status());
    }

    @Test
    void shouldRevokeById() {
        pending = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        when(invitationRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        invitationService.revoke(pending.getId(), companyId);
        assertEquals(InvitationStatus.REVOKED, pending.getStatus());
    }

    @Test
    void shouldRejectAcceptWhenAlreadyUsed() {
        Invitation used = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        used.accept();
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(used));

        assertThrows(IllegalStateException.class, () -> invitationService.accept("tok-abc", UUID.randomUUID()));
    }

    @Test
    void shouldRejectAcceptWhenRevoked() {
        Invitation revoked = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        revoked.revoke();
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThrows(IllegalStateException.class, () -> invitationService.accept("tok-abc", UUID.randomUUID()));
    }

    @Test
    void shouldRejectAcceptWhenAlreadyMemberOfTargetCompany() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(new Email("convite@empresa.com"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        pending = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));
        when(membershipRepository.existsActiveByUserIdAndCompanyId(userId, companyId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> invitationService.accept("tok-abc", userId));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void shouldAcceptInvitationWhenUserAlreadyBelongsToAnotherCompany() {
        UUID userId = UUID.randomUUID();
        UUID otherCompany = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(new Email("convite@empresa.com"));
        when(user.getCompanyId()).thenReturn(otherCompany); // ativa em OUTRA empresa
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        pending = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));
        // membro ativo de outra empresa, mas NÃO da empresa-alvo
        when(membershipRepository.existsActiveByUserIdAndCompanyId(userId, companyId)).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationResponse response = invitationService.accept("tok-abc", userId);

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        verify(membershipRepository).save(any(Membership.class));
        // não deve trocar a empresa ativa do usuário (permanece na outra)
        verify(user, never()).setCompanyId(any());
    }

    @Test
    void shouldSetActiveCompanyWhenUserHadNoCompany() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(new Email("convite@empresa.com"));
        when(user.getCompanyId()).thenReturn(null); // sem empresa ativa (onboarding pendente)
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        pending = Invitation.create(companyId, "convite@empresa.com", "AGENT", InvitationTokenService.hash("tok-abc"), invitedBy);
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));
        when(membershipRepository.existsActiveByUserIdAndCompanyId(userId, companyId)).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationResponse response = invitationService.accept("tok-abc", userId);

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        verify(user).setCompanyId(companyId); // empresa convidada vira a ativa
        verify(membershipRepository).save(any(Membership.class));
        verify(user).grantCrmAccess();
        verify(userRepository).save(user);
    }

    private void setExpired(Invitation invitation) {
        // mutate expiresAt para o passado via reflexão simples em teste
        try {
            var f = Invitation.class.getDeclaredField("expiresAt");
            f.setAccessible(true);
            f.set(invitation, java.time.LocalDateTime.now().minusDays(1));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}