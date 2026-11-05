package com.becommerce.crm.communication.telegram.web;

import com.becommerce.crm.communication.telegram.application.TelegramConfigService;
import com.becommerce.crm.communication.telegram.application.TelegramConfigService.TelegramConfigResponse;
import com.becommerce.crm.communication.telegram.application.TelegramLinkService;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Configuração do bot do Telegram da clínica (o token nunca volta nas respostas). */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/telegram")
public class TelegramSettingsController {

    private static final String OWN_COMPANY = "Você só pode configurar o Telegram da sua própria empresa.";

    private final TelegramConfigService config;
    private final TelegramLinkService links;

    public TelegramSettingsController(TelegramConfigService config, TelegramLinkService links) {
        this.config = config;
        this.links = links;
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

    public record LinkCodeResponse(String code, String botUsername, int expiresInMinutes) {}

    @PostMapping("/link-code")
    @PreAuthorize("hasAuthority('ai:chat')")
    public LinkCodeResponse linkCode(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                     @AuthenticationPrincipal CurrentUser principal) {
        TelegramConfigResponse cfg = config.get(companyId);
        if (!cfg.configured() || !cfg.enabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O Telegram ainda não foi configurado nesta clínica.");
        }
        return new LinkCodeResponse(links.generateCode(companyId, principal.userId()), cfg.botUsername(), 10);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<TelegramLinkService.LinkView> me(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                           @AuthenticationPrincipal CurrentUser principal) {
        return links.myLink(companyId, principal.userId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<Void> unlinkMe(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                         @AuthenticationPrincipal CurrentUser principal) {
        links.unlinkUser(companyId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/links")
    @PreAuthorize("hasAuthority('company:update')")
    public List<TelegramLinkService.LinkView> listLinks(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return links.listLinks(companyId);
    }

    @DeleteMapping("/links/{userId}")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<Void> revoke(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID userId) {
        links.unlinkUser(companyId, userId);
        return ResponseEntity.noContent().build();
    }
}
