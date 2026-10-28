package com.becommerce.crm.masterdata.company.mail;

import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.MailSettingsResponse;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.SaveMailSettings;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * E-mail da própria clínica: a clínica informa e-mail e senha (no Gmail, senha
 * de app) e os formulários saem dessa conta. Servidor e porta são deduzidos dos
 * provedores comuns; nos demais a clínica informa.
 */
@Service
public class CompanyMailService {

    record Server(String host, int port) {
    }

    private static final Map<String, Server> KNOWN = Map.of(
            "gmail.com", new Server("smtp.gmail.com", 587),
            "googlemail.com", new Server("smtp.gmail.com", 587),
            "outlook.com", new Server("smtp-mail.outlook.com", 587),
            "hotmail.com", new Server("smtp-mail.outlook.com", 587),
            "live.com", new Server("smtp-mail.outlook.com", 587),
            "yahoo.com", new Server("smtp.mail.yahoo.com", 587),
            "yahoo.com.br", new Server("smtp.mail.yahoo.com", 587),
            "icloud.com", new Server("smtp.mail.me.com", 587),
            "uol.com.br", new Server("smtps.uol.com.br", 587),
            "bol.com.br", new Server("smtps.bol.com.br", 587));

    private final CompanyMailSettingsJpaRepository repository;
    private final MailPasswordCipher cipher;

    public CompanyMailService(CompanyMailSettingsJpaRepository repository, MailPasswordCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    static Optional<Server> knownServer(String email) {
        int at = email == null ? -1 : email.lastIndexOf('@');
        return at < 0 ? Optional.empty()
                : Optional.ofNullable(KNOWN.get(email.substring(at + 1).toLowerCase(Locale.ROOT)));
    }

    @Transactional(readOnly = true)
    public MailSettingsResponse get(UUID companyId) {
        return inTenant(companyId, () -> repository.findById(companyId)
                .map(s -> new MailSettingsResponse(true, s.getEmail(), s.getSenderName(), s.getHost(),
                        s.getPort(), s.getPasswordCipher() != null, s.getUpdatedAt()))
                .orElse(new MailSettingsResponse(false, null, null, null, null, false, null)));
    }

    @Transactional(readOnly = true)
    public boolean isConfigured(UUID companyId) {
        return inTenant(companyId, () -> repository.existsById(companyId));
    }

    @Transactional
    public MailSettingsResponse save(UUID companyId, SaveMailSettings request, UUID actorId) {
        String email = request.email().trim();
        Server known = knownServer(email).orElse(null);
        String host = blank(request.host()) ? (known == null ? null : known.host()) : request.host().trim();
        int port = request.port() != null ? request.port() : known != null ? known.port() : 587;
        if (host == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o servidor SMTP do seu provedor de e-mail.");
        }
        // Senha de app do Gmail é exibida em blocos com espaços.
        String password = blank(request.password()) ? null : request.password().replace(" ", "");
        inTenant(companyId, () -> {
            CompanyMailSettingsJpaEntity settings = repository.findById(companyId)
                    .orElseGet(() -> new CompanyMailSettingsJpaEntity(companyId));
            if (password == null && settings.getPasswordCipher() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a senha do e-mail.");
            }
            settings.update(email, blank(request.senderName()) ? null : request.senderName().trim(),
                    host, port, password == null ? null : cipher.encrypt(password), actorId);
            return repository.save(settings);
        });
        return get(companyId);
    }

    @Transactional
    public void delete(UUID companyId) {
        inTenant(companyId, () -> {
            repository.deleteById(companyId);
            return null;
        });
    }

    /** Envio de teste: confere usuário/senha e devolve uma explicação quando falha. */
    @Transactional(readOnly = true)
    public void sendTest(UUID companyId, String to) {
        CompanyMailSettingsJpaEntity settings = require(companyId);
        String target = blank(to) ? settings.getEmail() : to.trim();
        try {
            send(settings, target, "Teste de e-mail do CRM",
                    "<p>Tudo certo! Os formulários para os pacientes vão sair deste e-mail.</p>", null);
        } catch (MailException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, explain(e));
        }
    }

    /** Envia o link do formulário pela conta da clínica. */
    @Transactional(readOnly = true)
    public void sendPatientForm(UUID companyId, String to, String clinicName, String patientName,
                                String formName, String url) {
        CompanyMailSettingsJpaEntity settings = require(companyId);
        String from = blank(settings.getSenderName()) ? clinicName : settings.getSenderName();
        String body = """
                <p>Olá, %s!</p>
                <p><strong>%s</strong> pediu que você responda o formulário <strong>%s</strong>
                antes do atendimento. Leva poucos minutos e você assina no final.</p>
                <p><a href="%s">Responder o formulário</a></p>
                <p>O link é pessoal e vale por 7 dias. Dúvidas? Basta responder este e-mail.</p>
                """.formatted(escape(patientName), escape(from), escape(formName), url);
        send(settings, to, from + ": " + formName, body, from);
    }

    private CompanyMailSettingsJpaEntity require(UUID companyId) {
        return inTenant(companyId, () -> repository.findById(companyId)).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configure o e-mail da clínica primeiro."));
    }

    private void send(CompanyMailSettingsJpaEntity settings, String to, String subject, String html,
                      String fromName) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getHost());
        sender.setPort(settings.getPort());
        sender.setUsername(settings.getEmail());
        sender.setPassword(cipher.decrypt(settings.getPasswordCipher()));
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        if (settings.getPort() == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (blank(fromName)) {
                helper.setFrom(settings.getEmail());
            } else {
                helper.setFrom(settings.getEmail(), fromName);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Falha ao montar o e-mail: " + e.getMessage(), e);
        }
    }

    static String explain(Exception e) {
        String message = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
        if (message.contains("authentication") || message.contains("535") || message.contains("username")) {
            return "E-mail ou senha recusados pelo provedor. No Gmail, use uma senha de app.";
        }
        if (message.contains("connect") || message.contains("timeout") || message.contains("unknownhost")) {
            return "Não foi possível conectar ao servidor de e-mail. Confira o servidor e a porta.";
        }
        return "Falha ao enviar: " + e.getMessage();
    }

    private static <T> T inTenant(UUID companyId, Supplier<T> action) {
        UUID previous = TenantContext.hasCompanyId() ? TenantContext.getCompanyId() : null;
        TenantContext.setCompanyId(companyId);
        try {
            return action.get();
        } finally {
            if (previous != null) {
                TenantContext.setCompanyId(previous);
            } else {
                TenantContext.clear();
            }
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
