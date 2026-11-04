package com.becommerce.crm.masterdata.company.mail;

import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InvitePreview;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteSuggestion;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteSuggestionRequest;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteTemplate;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.MailSettingsResponse;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.SaveMailSettings;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.TestMail;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** E-mail da clínica (SMTP próprio) usado para enviar formulários aos pacientes. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/mail-settings")
public class CompanyMailController {

    private static final String OWN_COMPANY = "Você só pode configurar o e-mail da sua própria empresa.";

    private final CompanyMailService service;
    private final InviteTemplateLeoService leo;

    public CompanyMailController(CompanyMailService service, InviteTemplateLeoService leo) {
        this.service = service;
        this.leo = leo;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<MailSettingsResponse> get(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return ResponseEntity.ok(service.get(companyId));
    }

    /** Só diz se há e-mail configurado: quem convida precisa saber antes de enviar. */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('company:update') or hasAuthority('membership:manage')")
    public ResponseEntity<Map<String, Boolean>> status(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return ResponseEntity.ok(Map.of("configured", service.isConfigured(companyId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<MailSettingsResponse> save(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                     @Valid @RequestBody SaveMailSettings request,
                                                     @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.save(companyId, request, principal.userId()));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<Void> delete(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        service.delete(companyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<Void> test(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                     @Valid @RequestBody(required = false) TestMail request) {
        service.sendTest(companyId, request == null ? null : request.to());
        return ResponseEntity.noContent().build();
    }

    /** Modelo do e-mail de convite (logo, cor, título, mensagem e rodapé). */
    @PutMapping("/invite-template")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<MailSettingsResponse> saveInviteTemplate(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                                   @Valid @RequestBody InviteTemplate request,
                                                                   @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.saveInviteTemplate(companyId, request, principal.userId()));
    }

    /** Prévia com os valores da tela, sem salvar. */
    @PostMapping("/invite-template/preview")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<InvitePreview> previewInviteTemplate(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                               @Valid @RequestBody InviteTemplate request) {
        return ResponseEntity.ok(service.previewInvite(companyId, request));
    }

    /** Se o Leo (IA) está disponível para sugerir os textos do convite. */
    @GetMapping("/invite-template/leo")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<Map<String, Boolean>> leoStatus(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return ResponseEntity.ok(Map.of("available", leo.isAvailable()));
    }

    /** Leo sugere título, mensagem e rodapé; nada é salvo. */
    @PostMapping("/invite-template/suggest")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<InviteSuggestion> suggestInviteTemplate(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                                  @Valid @RequestBody InviteSuggestionRequest request,
                                                                  @AuthenticationPrincipal CurrentUser principal) {
        String companyName = service.get(companyId).senderName();
        return ResponseEntity.ok(leo.suggest(companyId, principal.userId(), companyName, request));
    }

    /** Envia um convite de exemplo com o modelo salvo. */
    @PostMapping("/invite-template/test")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<Void> testInviteTemplate(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                   @Valid @RequestBody(required = false) TestMail request) {
        service.sendInviteTest(companyId, request == null ? null : request.to());
        return ResponseEntity.noContent().build();
    }
}
