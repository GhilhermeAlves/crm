package com.becommerce.crm.masterdata.company.mail;

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

import java.util.UUID;

/** E-mail da clínica (SMTP próprio) usado para enviar formulários aos pacientes. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/mail-settings")
public class CompanyMailController {

    private static final String OWN_COMPANY = "Você só pode configurar o e-mail da sua própria empresa.";

    private final CompanyMailService service;

    public CompanyMailController(CompanyMailService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<MailSettingsResponse> get(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return ResponseEntity.ok(service.get(companyId));
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
}
