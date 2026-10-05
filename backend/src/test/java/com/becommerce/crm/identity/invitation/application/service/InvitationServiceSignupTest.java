package com.becommerce.crm.identity.invitation.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.communication.notification.application.EmailSender;
import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.application.service.AuthService;
import com.becommerce.crm.identity.domain.Role;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.DuplicateEmailException;
import com.becommerce.crm.identity.domain.valueobject.Email;
import com.becommerce.crm.identity.invitation.application.dto.CreateInvitationRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationLinkResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationPreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationResponse;
import com.becommerce.crm.identity.invitation.application.port.output.InvitationRepository;
import com.becommerce.crm.identity.invitation.domain.Invitation;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNotFoundException;
import com.becommerce.crm.identity.invitation.infrastructure.persistence.InvitationTokenContextHolder;
import com.becommerce.crm.identity.invitation.infrastructure.rate.InvitationRateLimiter;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.identity.membership.domain.Membership;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.masterdata.quota.domain.exception.QuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fase 1 do convite: prévia pública, cadastro de quem não tem conta, aceite
 * com lock e regeneração do link.
 */
@ExtendWith(MockitoExtension.class)
class InvitationServiceSignupTest {

    private static final String TOKEN = "tok-abc";
    private static final String EMAIL = "convidado@empresa.com";

    @Mock InvitationRepository invitationRepository;
    @Mock CompanyRepository companyRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock EmailSender emailSender;
    @Mock InvitationTokenContextHolder tokenContext;
    @Mock InvitationRateLimiter rateLimiter;
    @Mock TenantAuditRecorder auditor;
    @Mock AuthService authService;

    InvitationService service;

    private UUID companyId;
    private UUID invitedBy;

    @BeforeEach
    void setUp() {
        service = new InvitationService(invitationRepository, companyRepository, membershipRepository,
                userRepository, roleRepository, userRoleRepository, emailSender, tokenContext,
                rateLimiter, auditor, authService, "https://crm.exemplo.com");
        companyId = UUID.randomUUID();
        invitedBy = UUID.randomUUID();
        lenient().when(rateLimiter.tryAccept(anyString())).thenReturn(true);
        lenient().when(rateLimiter.tryCreate(anyString())).thenReturn(true);
        lenient().when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Company activeCompany(int maxUsers) {
        return Company.create(
                "Empresa LTDA", "EmpresaX", "12345678000190",
                "123456789", "987654321",
                "admin@empresa.com", "(11) 99999-0000", null,
                "01001000", "Rua X", "1", null,
                "Centro", "SP", "SP", "Brasil",
                CompanyPlan.STARTER, maxUsers, 1024, 500, null, null);
    }

    private Invitation pending() {
        return Invitation.create(companyId, EMAIL, "Fulano", "MANAGER",
                InvitationTokenService.hash(TOKEN), invitedBy);
    }

    private void lockReturns(Invitation invitation) {
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenService.hash(TOKEN)))
                .thenReturn(Optional.of(invitation));
    }

    private static void expire(Invitation invitation) {
        try {
            var f = Invitation.class.getDeclaredField("expiresAt");
            f.setAccessible(true);
            f.set(invitation, LocalDateTime.now().minusDays(1));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Invitation accepted() {
        Invitation i = pending();
        i.accept();
        return i;
    }

    private Invitation revoked() {
        Invitation i = pending();
        i.revoke();
        return i;
    }

    private Invitation expired() {
        Invitation i = pending();
        expire(i);
        return i;
    }

    // ---------------------------------------------------------------- prévia

    @Test
    void preview_showsCompanyEmailNameAndRole() {
        when(invitationRepository.findByTokenHash(InvitationTokenService.hash(TOKEN))).thenReturn(Optional.of(pending()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        InvitationPreviewResponse preview = service.preview(TOKEN);

        assertEquals("EmpresaX", preview.companyName());
        assertEquals(EMAIL, preview.email());
        assertEquals("Fulano", preview.inviteeName());
        assertEquals("MANAGER", preview.role());
        assertEquals(InvitationStatus.PENDING, preview.status());
        assertFalse(preview.hasAccount());
        verify(tokenContext).setTokenHash(InvitationTokenService.hash(TOKEN));
    }

    @Test
    void preview_reportsExistingAccount() {
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertTrue(service.preview(TOKEN).hasAccount());
    }

    @Test
    void preview_pendingPastExpiry_isReportedAsExpired() {
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));

        assertEquals(InvitationStatus.EXPIRED, service.preview(TOKEN).status());
    }

    @Test
    void preview_acceptedAndRevokedReportTheirStatus() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));
        when(invitationRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(accepted()))
                .thenReturn(Optional.of(revoked()));

        assertEquals(InvitationStatus.ACCEPTED, service.preview(TOKEN).status());
        assertEquals(InvitationStatus.REVOKED, service.preview(TOKEN).status());
    }

    @Test
    void preview_unknownToken_isNotFound() {
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvitationNotFoundException.class, () -> service.preview("nope"));
    }

    // -------------------------------------------- pré-validação do cadastro

    @Test
    void prepareSignup_pendingInvitationWithoutAccount_returnsInvitation() {
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending()));
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        InvitationResponse response = service.prepareSignup(TOKEN);

        assertEquals(EMAIL, response.email());
        assertEquals(InvitationStatus.PENDING, response.status());
    }

    @Test
    void prepareSignup_rejectsAcceptedExpiredAndRevoked() {
        when(invitationRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(accepted()))
                .thenReturn(Optional.of(expired()))
                .thenReturn(Optional.of(revoked()));

        assertEquals(InvitationStatus.ACCEPTED, assertThrows(InvitationNoLongerValidException.class,
                () -> service.prepareSignup(TOKEN)).getStatus());
        assertEquals(InvitationStatus.EXPIRED, assertThrows(InvitationNoLongerValidException.class,
                () -> service.prepareSignup(TOKEN)).getStatus());
        assertEquals(InvitationStatus.REVOKED, assertThrows(InvitationNoLongerValidException.class,
                () -> service.prepareSignup(TOKEN)).getStatus());
    }

    @Test
    void prepareSignup_existingUser_mustLogInInstead() {
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending()));
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.prepareSignup(TOKEN));
    }

    // ------------------------------------------------ cadastro (transação)

    @Test
    void completeSignup_createsUserMembershipRoleAndAcceptsInvitation() {
        Invitation invitation = pending();
        lockReturns(invitation);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));
        User created = mock(User.class);
        UUID userId = UUID.randomUUID();
        when(created.getId()).thenReturn(userId);
        when(created.getCompanyId()).thenReturn(null);
        when(authService.provisionInvitedUser("kc-1", EMAIL, "$hash", "Fulano de Tal")).thenReturn(created);
        Role manager = mock(Role.class);
        UUID roleId = UUID.randomUUID();
        when(manager.getId()).thenReturn(roleId);
        when(roleRepository.findByNameAndCompanyId("MANAGER", companyId)).thenReturn(Optional.of(manager));

        InvitationResponse response = service.completeSignup(TOKEN, "kc-1", "$hash", "Fulano de Tal");

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        ArgumentCaptor<Membership> membership = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(membership.capture());
        assertEquals(companyId, membership.getValue().getCompanyId());
        assertEquals("MANAGER", membership.getValue().getRole());
        verify(userRoleRepository).save(any());
        verify(created).setCompanyId(companyId);
        verify(created).grantCrmAccess();
    }

    @Test
    void completeSignup_invitationAcceptedByConcurrentRequest_failsWithoutCreatingUser() {
        lockReturns(accepted());

        InvitationNoLongerValidException ex = assertThrows(InvitationNoLongerValidException.class,
                () -> service.completeSignup(TOKEN, "kc-1", "$hash", "Fulano"));

        assertEquals(InvitationStatus.ACCEPTED, ex.getStatus());
        verify(authService, never()).provisionInvitedUser(anyString(), anyString(), anyString(), anyString());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void completeSignup_emailRegisteredByConcurrentRequest_failsWithoutCreatingUser() {
        lockReturns(pending());
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> service.completeSignup(TOKEN, "kc-1", "$hash", "Fulano"));
        verify(authService, never()).provisionInvitedUser(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void completeSignup_userLimitReached_failsBeforeMembership() {
        lockReturns(pending());
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        User created = mock(User.class);
        when(created.getId()).thenReturn(UUID.randomUUID());
        when(authService.provisionInvitedUser(anyString(), anyString(), anyString(), anyString())).thenReturn(created);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(2)));
        when(membershipRepository.countActiveByCompanyId(companyId)).thenReturn(2L);

        assertThrows(QuotaExceededException.class,
                () -> service.completeSignup(TOKEN, "kc-1", "$hash", "Fulano"));
        verify(membershipRepository, never()).save(any());
    }

    // ------------------------------------------- aceite (usuário com conta)

    private User existingUser(UUID userId, UUID activeCompany) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getEmail()).thenReturn(new Email(EMAIL));
        lenient().when(user.getCompanyId()).thenReturn(activeCompany);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void accept_locksInvitationRow() {
        UUID userId = UUID.randomUUID();
        existingUser(userId, null);
        lockReturns(pending());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));

        service.accept(TOKEN, userId);

        verify(invitationRepository).findByTokenHashForUpdate(InvitationTokenService.hash(TOKEN));
        verify(invitationRepository, never()).findByTokenHash(anyString());
    }

    @Test
    void accept_secondConcurrentAccept_seesAcceptedAndFails() {
        lockReturns(accepted());

        InvitationNoLongerValidException ex = assertThrows(InvitationNoLongerValidException.class,
                () -> service.accept(TOKEN, UUID.randomUUID()));

        assertEquals(InvitationStatus.ACCEPTED, ex.getStatus());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void accept_expiredAndRevoked_fail() {
        when(invitationRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(expired()))
                .thenReturn(Optional.of(revoked()));

        assertEquals(InvitationStatus.EXPIRED, assertThrows(InvitationNoLongerValidException.class,
                () -> service.accept(TOKEN, UUID.randomUUID())).getStatus());
        assertEquals(InvitationStatus.REVOKED, assertThrows(InvitationNoLongerValidException.class,
                () -> service.accept(TOKEN, UUID.randomUUID())).getStatus());
    }

    @Test
    void accept_userWithoutCompany_getsInvitingCompanyAsActive() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, null);
        lockReturns(pending());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));

        service.accept(TOKEN, userId);

        verify(user).setCompanyId(companyId);
        verify(membershipRepository).save(any(Membership.class));
    }

    @Test
    void accept_userFromAnotherCompany_keepsActiveCompanyAndGainsMembership() {
        UUID userId = UUID.randomUUID();
        UUID otherCompany = UUID.randomUUID();
        User user = existingUser(userId, otherCompany);
        lockReturns(pending());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));

        InvitationResponse response = service.accept(TOKEN, userId);

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        ArgumentCaptor<Membership> membership = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(membership.capture());
        assertEquals(companyId, membership.getValue().getCompanyId());
        verify(user, never()).setCompanyId(any());
    }

    // --------------------------------------------------------- criação

    @Test
    void create_storesInviteeNameAndLinksToConviteRoute() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));
        when(invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING)).thenReturn(List.of());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        InvitationResponse response = service.create(companyId,
                new CreateInvitationRequest(EMAIL, "AGENT", "Fulano"), invitedBy);

        assertEquals("Fulano", response.inviteeName());
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendInvitation(eq(EMAIL), eq("EmpresaX"), eq("AGENT"), url.capture());
        assertTrue(url.getValue().startsWith("https://crm.exemplo.com/convite/"), url.getValue());
    }

    // ------------------------------------------------------ regeneração

    @Test
    void regenerate_pending_issuesNewTokenRenewsExpiryAndSendsEmail() {
        Invitation invitation = pending();
        String oldHash = invitation.getTokenHash();
        try {
            var f = Invitation.class.getDeclaredField("expiresAt");
            f.setAccessible(true);
            f.set(invitation, LocalDateTime.now().plusDays(1));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(activeCompany(5)));

        InvitationLinkResponse link = service.regenerate(companyId, invitation.getId(), true, invitedBy);

        assertNotEquals(oldHash, invitation.getTokenHash());
        assertTrue(invitation.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
        assertTrue(link.url().startsWith("https://crm.exemplo.com/convite/"));
        String token = link.url().substring(link.url().lastIndexOf('/') + 1);
        assertEquals(invitation.getTokenHash(), InvitationTokenService.hash(token));
        verify(emailSender).sendInvitation(eq(EMAIL), eq("EmpresaX"), eq("MANAGER"), eq(link.url()));
    }

    @Test
    void regenerate_copyLinkOnly_doesNotSendEmail() {
        Invitation invitation = pending();
        when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));

        service.regenerate(companyId, invitation.getId(), false, invitedBy);

        verify(emailSender, never()).sendInvitation(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void regenerate_expired_becomesPendingAgain() {
        Invitation invitation = expired();
        invitation.markExpired();
        when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
        when(invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING)).thenReturn(List.of());
        service.regenerate(companyId, invitation.getId(), false, invitedBy);

        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
    }

    @Test
    void regenerate_acceptedOrRevoked_isRejected() {
        Invitation a = accepted();
        Invitation r = revoked();
        when(invitationRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(invitationRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThrows(InvitationNoLongerValidException.class,
                () -> service.regenerate(companyId, a.getId(), false, invitedBy));
        assertThrows(InvitationNoLongerValidException.class,
                () -> service.regenerate(companyId, r.getId(), false, invitedBy));
    }

    @Test
    void regenerate_invitationOfAnotherCompany_isNotFound() {
        Invitation invitation = pending();
        when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));

        assertThrows(InvitationNotFoundException.class,
                () -> service.regenerate(UUID.randomUUID(), invitation.getId(), false, invitedBy));
    }
}
