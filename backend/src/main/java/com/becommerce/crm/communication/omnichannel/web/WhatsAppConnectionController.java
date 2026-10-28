package com.becommerce.crm.communication.omnichannel.web;

import com.becommerce.crm.communication.omnichannel.application.service.WhatsAppConnectionService;
import com.becommerce.crm.communication.omnichannel.application.service.WhatsAppConnectionService.ConnectionQr;
import com.becommerce.crm.communication.omnichannel.application.service.WhatsAppConnectionService.ConnectionStatus;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conexão do WhatsApp da empresa ativa por QR code (Evolution). Mesmo escopo do
 * {@code OmnichannelChannelController}: a empresa vem do usuário logado.
 */
@RestController
@RequestMapping("/api/v1/omnichannel/whatsapp/connection")
public class WhatsAppConnectionController {

    private final WhatsAppConnectionService service;

    public WhatsAppConnectionController(WhatsAppConnectionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('omnichannel:read')")
    public ResponseEntity<ConnectionStatus> status(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.status(principal.companyId()));
    }

    @PostMapping("/connect")
    @PreAuthorize("hasAuthority('omnichannel:update')")
    public ResponseEntity<ConnectionQr> connect(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.connect(principal.companyId()));
    }

    @PostMapping("/disconnect")
    @PreAuthorize("hasAuthority('omnichannel:update')")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal CurrentUser principal) {
        service.disconnect(principal.companyId());
        return ResponseEntity.noContent().build();
    }
}
