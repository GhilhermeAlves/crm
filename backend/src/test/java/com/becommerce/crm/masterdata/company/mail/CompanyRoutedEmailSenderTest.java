package com.becommerce.crm.masterdata.company.mail;

import com.becommerce.crm.communication.notification.application.EmailSender;
import com.becommerce.crm.identity.application.port.output.EmailService;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyRoutedEmailSenderTest {

    private final UUID companyId = UUID.randomUUID();
    private final CompanyMailService companyMail = mock(CompanyMailService.class);
    private final EmailSender fallbackSender = mock(EmailSender.class);
    private final EmailService fallbackService = mock(EmailService.class);
    private final UserRepository users = mock(UserRepository.class);
    private CompanyRoutedEmailSender sender;

    @BeforeEach
    void setUp() {
        sender = new CompanyRoutedEmailSender(companyMail, List.of(fallbackSender), List.of(fallbackService),
                users, "https://app.test/");
        TenantContext.setCompanyId(companyId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void invitationGoesThroughCompanyMail() {
        when(companyMail.isConfigured(companyId)).thenReturn(true);
        when(companyMail.sendInvitation(companyId, "a@b.com", "Clínica", "AGENT", "url")).thenReturn(true);

        sender.sendInvitation("a@b.com", "Clínica", "AGENT", "url");

        verify(fallbackSender, never()).sendInvitation(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void invitationAsksToConfigureMailFirst() {
        assertThatThrownBy(() -> sender.sendInvitation("a@b.com", "Clínica", "AGENT", "url"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cadastre o e-mail da empresa");

        verify(fallbackSender, never()).sendInvitation(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void sendFailureDoesNotBreakTheInvitation() {
        when(companyMail.isConfigured(companyId)).thenReturn(true);
        when(companyMail.sendInvitation(any(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new MailAuthenticationException("Authentication failed"));

        assertThatCode(() -> sender.sendInvitation("a@b.com", "Clínica", "AGENT", "url"))
                .doesNotThrowAnyException();
    }

    @Test
    void passwordResetUsesTheUsersCompany() {
        User user = mock(User.class);
        when(user.getCompanyId()).thenReturn(companyId);
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(companyMail.sendPasswordReset(eq(companyId), eq("a@b.com"), anyString())).thenReturn(true);

        sender.sendPasswordResetEmail("a@b.com", "tok");

        verify(companyMail).sendPasswordReset(eq(companyId), eq("a@b.com"),
                contains("https://app.test/reset-password?token=tok"));
        verify(fallbackService, never()).sendPasswordResetEmail(anyString(), anyString());
    }
}
