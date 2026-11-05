package com.becommerce.crm.communication.telegram.web;

import com.becommerce.crm.communication.telegram.application.TelegramConfigService;
import com.becommerce.crm.communication.telegram.application.TelegramConfigService.TelegramConfigResponse;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Configuração do bot do Telegram da clínica (o token nunca volta nas respostas). */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/telegram")
public class TelegramSettingsController {

    private static final String OWN_COMPANY = "Você só pode configurar o Telegram da sua própria empresa.";

    private final TelegramConfigService config;

    public TelegramSettingsController(TelegramConfigService config) {
        this.config = config;
    }

    public record SaveTelegramToken(@NotBlank String token) {}

    @GetMapping
    @PreAuthorize("hasAuthority('company:update')")
    public TelegramConfigResponse get(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return config.get(companyId);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('company:update')")
    public TelegramConfigResponse save(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                       @Valid @RequestBody SaveTelegramToken body,
                                       @AuthenticationPrincipal CurrentUser principal) {
        return config.save(companyId, body.token(), principal.userId());
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<Void> delete(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        config.delete(companyId);
        return ResponseEntity.noContent().build();
    }
}
