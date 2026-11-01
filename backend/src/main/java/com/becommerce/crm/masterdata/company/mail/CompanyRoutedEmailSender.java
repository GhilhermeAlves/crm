package com.becommerce.crm.masterdata.company.mail;

import com.becommerce.crm.communication.notification.application.EmailSender;
import com.becommerce.crm.identity.application.port.output.EmailService;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Todo e-mail do sistema sai da conta configurada na página de e-mail da
 * empresa. Convites usam a empresa do contexto (quem convida); a redefinição de
 * senha usa a empresa do usuário. Convite exige a configuração (400 pedindo o
 * cadastro); a redefinição de senha, anônima, cai no envio global
 * ({@code SmtpEmailSender}/console). Falha de envio não derruba a operação: o
 * convite continua salvo e o link fica disponível para copiar ou mandar por WhatsApp.
 */
@Service
@Primary
public class CompanyRoutedEmailSender implements EmailSender, EmailService {

    private static final Logger log = LoggerFactory.getLogger(CompanyRoutedEmailSender.class);

    static final String MAIL_NOT_CONFIGURED =
            "Cadastre o e-mail da empresa em Configurações > E-mail antes de enviar convites por e-mail.";

    private final CompanyMailService companyMail;
    private final EmailSender fallbackSender;
    private final EmailService fallbackService;
    private final UserRepository userRepository;
    private final String frontendBaseUrl;

    public CompanyRoutedEmailSender(CompanyMailService companyMail,
                                    List<EmailSender> senders,
                                    List<EmailService> services,
                                    UserRepository userRepository,
                                    @Value("${app.mail.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.companyMail = companyMail;
        this.fallbackSender = senders.stream().filter(s -> !(s instanceof CompanyRoutedEmailSender))
                .findFirst().orElseThrow();
        this.fallbackService = services.stream().filter(s -> !(s instanceof CompanyRoutedEmailSender))
                .findFirst().orElseThrow();
        this.userRepository = userRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void sendInvitation(String to, String companyName, String role, String tokenUrl) {
        UUID companyId = TenantContext.hasCompanyId() ? TenantContext.getCompanyId() : null;
        if (companyId == null || !companyMail.isConfigured(companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MAIL_NOT_CONFIGURED);
        }
        try {
            companyMail.sendInvitation(companyId, to, companyName, role, tokenUrl);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar convite para {} (empresa={}): {}", to, companyId, e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String link = frontendBaseUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        try {
            UUID companyId = userRepository.findByEmail(to).map(User::getCompanyId).orElse(null);
            if (!companyMail.sendPasswordReset(companyId, to, link)) {
                fallbackService.sendPasswordResetEmail(to, token);
            }
        } catch (RuntimeException e) {
            log.error("Falha ao enviar redefinição de senha para {}: {}", to, e.getMessage());
        }
    }

    @Override
    public boolean deliversEmail() {
        return fallbackSender.deliversEmail();
    }

    @Override
    public void sendPatientForm(String to, String clinicName, String replyTo, String patientName,
                                String formName, String url) {
        // O formulário já escolhe a conta da clínica em PatientFormRequestService.
        fallbackSender.sendPatientForm(to, clinicName, replyTo, patientName, formName, url);
    }
}
