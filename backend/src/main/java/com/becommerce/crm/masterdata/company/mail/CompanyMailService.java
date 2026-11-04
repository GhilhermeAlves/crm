package com.becommerce.crm.masterdata.company.mail;

import com.becommerce.crm.communication.notification.application.InvitationEmailTemplate;
import com.becommerce.crm.communication.notification.application.InvitationEmailTemplate.Custom;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InvitePreview;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteTemplate;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.MailSettingsResponse;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.SaveMailSettings;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
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
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /** Imagem anexada ao e-mail e referenciada no HTML por {@code cid:}. */
    record InlineImage(String contentId, byte[] bytes, String contentType) {
    }

    /** E-mail de convite montado (assunto, HTML e logo inline opcional). */
    record InviteMail(String subject, String html, InlineImage logo) {
    }

    static final String INVITE_LOGO_CID = "invite-logo";
    static final int INVITE_LOGO_MAX_BYTES = 500 * 1024;
    private static final Pattern LOGO_DATA_URL =
            Pattern.compile("^data:(image/(?:png|jpeg));base64,([A-Za-z0-9+/=\\s]+)$");

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
    private final String frontendBaseUrl;
    /** Contas de envio (e-mail SMTP da empresa) que usam o template da PG Automação. */
    private final java.util.Set<String> pgBrandedSenders;

    public CompanyMailService(CompanyMailSettingsJpaRepository repository, MailPasswordCipher cipher,
                              @org.springframework.beans.factory.annotation.Value(
                                      "${app.mail.frontend-base-url:http://localhost:3000}") String frontendBaseUrl,
                              @org.springframework.beans.factory.annotation.Value(
                                      "${app.mail.pg-branded-senders:ghilherme007@gmail.com}") String pgBrandedSenders) {
        this.repository = repository;
        this.cipher = cipher;
        this.frontendBaseUrl = frontendBaseUrl;
        this.pgBrandedSenders = java.util.Arrays.stream(pgBrandedSenders.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
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
                        s.getPort(), s.getPasswordCipher() != null, s.getUpdatedAt(),
                        new InviteTemplate(s.isInviteEnabled(), s.getInviteLogo(), s.getInviteColor(),
                                s.getInviteTitle(), s.getInviteMessage(), s.getInviteFooter())))
                .orElse(new MailSettingsResponse(false, null, null, null, null, false, null, null)));
    }

    /** Salva o modelo do convite; exige a conta de envio já configurada. */
    @Transactional
    public MailSettingsResponse saveInviteTemplate(UUID companyId, InviteTemplate request, UUID actorId) {
        InviteTemplate clean = sanitize(request);
        inTenant(companyId, () -> {
            CompanyMailSettingsJpaEntity settings = repository.findById(companyId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configure o e-mail de envio primeiro."));
            settings.updateInviteTemplate(clean.enabled(), clean.logo(), clean.color(), clean.title(),
                    clean.message(), clean.footer(), actorId);
            return repository.save(settings);
        });
        return get(companyId);
    }

    /**
     * Prévia do convite com os valores da tela (ainda não salvos). O logo vai como
     * data URL, que o navegador mostra; no envio real ele segue anexado (cid).
     */
    @Transactional(readOnly = true)
    public InvitePreview previewInvite(UUID companyId, InviteTemplate request) {
        InviteTemplate clean = sanitize(request);
        CompanyMailSettingsJpaEntity settings = require(companyId);
        InviteMail mail = buildInvite(settings.getEmail(), settings.getSenderName(), clean,
                sampleCompany(settings), "MANAGER", sampleUrl(), true);
        return new InvitePreview(mail.subject(), mail.html());
    }

    /** Envia um convite de exemplo com o modelo salvo. */
    @Transactional(readOnly = true)
    public void sendInviteTest(UUID companyId, String to) {
        CompanyMailSettingsJpaEntity settings = require(companyId);
        String target = blank(to) ? settings.getEmail() : to.trim();
        InviteMail mail = buildInvite(settings, sampleCompany(settings), "MANAGER", sampleUrl());
        try {
            send(settings, target, "[Teste] " + mail.subject(), mail.html(), settings.getSenderName(), mail.logo());
        } catch (MailException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, explain(e));
        }
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

    /**
     * Convite pela conta de e-mail da empresa. Devolve false quando a empresa
     * não configurou e-mail (quem chama decide o que fazer). Sem @Transactional:
     * uma falha de envio não pode marcar para rollback a transação de quem chama.
     */
    public boolean sendInvitation(UUID companyId, String to, String companyName, String role, String url) {
        Optional<CompanyMailSettingsJpaEntity> settings = find(companyId);
        if (settings.isEmpty()) {
            return false;
        }
        InviteMail mail = buildInvite(settings.get(), companyName, role, url);
        send(settings.get(), to, mail.subject(), mail.html(), settings.get().getSenderName(), mail.logo());
        return true;
    }

    /** Redefinição de senha pela conta de e-mail da empresa; false quando não há configuração. */
    public boolean sendPasswordReset(UUID companyId, String to, String link) {
        Optional<CompanyMailSettingsJpaEntity> settings = find(companyId);
        if (settings.isEmpty()) {
            return false;
        }
        String body = """
                <p>Olá!</p>
                <p>Recebemos uma solicitação para redefinir sua senha.</p>
                <p><a href="%s">Redefinir minha senha</a></p>
                <p>Se não foi você, ignore este e-mail.</p>
                """.formatted(link);
        send(settings.get(), to, "Redefinição de senha", body, settings.get().getSenderName());
        return true;
    }

    private Optional<CompanyMailSettingsJpaEntity> find(UUID companyId) {
        return companyId == null ? Optional.empty() : inTenant(companyId, () -> repository.findById(companyId));
    }

    private CompanyMailSettingsJpaEntity require(UUID companyId) {
        return inTenant(companyId, () -> repository.findById(companyId)).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configure o e-mail da clínica primeiro."));
    }

    private void send(CompanyMailSettingsJpaEntity settings, String to, String subject, String html,
                      String fromName) {
        send(settings, to, subject, html, fromName, null);
    }

    private void send(CompanyMailSettingsJpaEntity settings, String to, String subject, String html,
                      String fromName, InlineImage inline) {
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
            if (inline != null) {
                // Depois do setText: o anexo inline é referenciado no HTML por cid:.
                helper.addInline(inline.contentId(), new ByteArrayResource(inline.bytes()), inline.contentType());
            }
            sender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Falha ao montar o e-mail: " + e.getMessage(), e);
        }
    }

    private InviteMail buildInvite(CompanyMailSettingsJpaEntity s, String companyName, String role, String url) {
        InviteTemplate saved = new InviteTemplate(s.isInviteEnabled(), s.getInviteLogo(), s.getInviteColor(),
                s.getInviteTitle(), s.getInviteMessage(), s.getInviteFooter());
        return buildInvite(s.getEmail(), s.getSenderName(), saved, companyName, role, url, false);
    }

    /**
     * Escolhe o modelo: personalizado (se ativo), PG Automação (conta de envio
     * dela) ou simples. Na prévia o logo vai como data URL; no envio, como cid.
     */
    InviteMail buildInvite(String senderEmail, String senderName, InviteTemplate template, String companyName,
                           String role, String url, boolean preview) {
        if (template != null && template.enabled()) {
            InlineImage logo = blank(template.logo()) ? null : decodeLogo(template.logo());
            String logoSrc = logo == null ? null : preview ? template.logo() : "cid:" + INVITE_LOGO_CID;
            Custom custom = new Custom(logoSrc, template.color(), template.title(), template.message(),
                    template.footer());
            return new InviteMail(InvitationEmailTemplate.customSubject(senderName, companyName),
                    InvitationEmailTemplate.customHtml(custom, senderName, companyName, role, url),
                    preview ? null : logo);
        }
        boolean pgBranded = pgBrandedSenders.contains(
                senderEmail == null ? "" : senderEmail.trim().toLowerCase(Locale.ROOT));
        return pgBranded
                ? new InviteMail(InvitationEmailTemplate.subject(companyName),
                        InvitationEmailTemplate.html(frontendBaseUrl, companyName, role, url), null)
                : new InviteMail(InvitationEmailTemplate.plainSubject(companyName),
                        InvitationEmailTemplate.plainHtml(companyName, role, url), null);
    }

    /** Normaliza e valida o modelo: textos aparados, vazio vira null, cor e logo conferidos. */
    static InviteTemplate sanitize(InviteTemplate t) {
        String color = trimToNull(t.color());
        if (color != null && !InvitationEmailTemplate.isValidColor(color)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cor inválida. Use o formato #RRGGBB.");
        }
        String logo = trimToNull(t.logo());
        if (logo != null) {
            decodeLogo(logo);
        }
        return new InviteTemplate(t.enabled(), logo, color == null ? null : color.toLowerCase(Locale.ROOT),
                trimToNull(t.title()), trimToNull(t.message()), trimToNull(t.footer()));
    }

    static InlineImage decodeLogo(String dataUrl) {
        Matcher m = LOGO_DATA_URL.matcher(dataUrl);
        if (!m.matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O logo deve ser uma imagem PNG ou JPEG.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o logo.");
        }
        if (bytes.length > INVITE_LOGO_MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O logo deve ter até 500 KB.");
        }
        return new InlineImage(INVITE_LOGO_CID, bytes, m.group(1));
    }

    private static String sampleCompany(CompanyMailSettingsJpaEntity settings) {
        return blank(settings.getSenderName()) ? "Sua Empresa" : settings.getSenderName();
    }

    private String sampleUrl() {
        return frontendBaseUrl.replaceAll("/+$", "") + "/convite/exemplo";
    }

    private static String trimToNull(String value) {
        return blank(value) ? null : value.trim();
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
