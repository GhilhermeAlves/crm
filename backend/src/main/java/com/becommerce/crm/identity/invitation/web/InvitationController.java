package com.becommerce.crm.identity.invitation.web;

import com.becommerce.crm.identity.invitation.application.dto.CreateInvitationRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationLinkResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationPreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationRegisterRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationResponse;
import com.becommerce.crm.identity.invitation.application.port.input.InvitationUseCase;
import com.becommerce.crm.identity.invitation.application.service.InvitationSignupService;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Convites (Sprint 8.5). Acesso administrativo exige que o solicitante esteja
 * na própria empresa (ADMIN/OWNER, via permission membership:manage). Aceite e
 * recusa são token-based (qualquer usuário autenticado com o token correto).
 */
@RestController
@RequestMapping("/api/v1")
public class InvitationController {

    private final InvitationUseCase invitationUseCase;
    private final InvitationSignupService invitationSignupService;

    public InvitationController(InvitationUseCase invitationUseCase,
                                InvitationSignupService invitationSignupService) {
        this.invitationUseCase = invitationUseCase;
        this.invitationSignupService = invitationSignupService;
    }

    @PostMapping("/companies/{companyId}/invitations")
    @PreAuthorize("hasAuthority('membership:manage')")
    public ResponseEntity<InvitationResponse> create(
            @CurrentCompanyId("Você só pode administrar convites da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        InvitationResponse response = invitationUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/companies/{companyId}/invitations")
    @PreAuthorize("hasAuthority('membership:view')")
    public ResponseEntity<List<InvitationResponse>> list(
            @CurrentCompanyId("Você só pode administrar convites da sua própria empresa.") UUID companyId,
            @RequestParam(required = false) String status) {
        InvitationStatus filter = status == null || status.isBlank()
                ? null : InvitationStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(invitationUseCase.listByCompany(companyId, filter));
    }

    @DeleteMapping("/companies/{companyId}/invitations/{invitationId}")
    @PreAuthorize("hasAuthority('membership:manage')")
    public ResponseEntity<Void> revoke(
            @CurrentCompanyId("Você só pode administrar convites da sua própria empresa.") UUID companyId,
            @PathVariable UUID invitationId) {
        invitationUseCase.revoke(invitationId, companyId);
        return ResponseEntity.noContent().build();
    }

    /** Novo link (o anterior deixa de valer). {@code send=true}: reenviar; {@code false}: copiar link. */
    @PostMapping("/companies/{companyId}/invitations/{invitationId}/regenerate")
    @PreAuthorize("hasAuthority('membership:manage')")
    public ResponseEntity<InvitationLinkResponse> regenerate(
            @CurrentCompanyId("Você só pode administrar convites da sua própria empresa.") UUID companyId,
            @PathVariable UUID invitationId,
            @RequestParam(defaultValue = "true") boolean send,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(invitationUseCase.regenerate(companyId, invitationId, send, principal.userId()));
    }

    /** Público: prévia do convite em /convite/{token}. */
    @GetMapping("/invitations/preview")
    public ResponseEntity<InvitationPreviewResponse> preview(
            @RequestParam String token,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        return ResponseEntity.ok(invitationSignupService.preview(token, clientKey(realIp)));
    }

    /** Público: cadastro de quem ainda não tem conta. O e-mail vem do convite. */
    @PostMapping("/invitations/register")
    public ResponseEntity<InvitationResponse> register(
            @Valid @RequestBody InvitationRegisterRequest request,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationSignupService.register(request, clientKey(realIp)));
    }

    private static String clientKey(String realIp) {
        return realIp != null && !realIp.isBlank() ? realIp : "unknown";
    }

    @PostMapping("/invitations/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> accept(@RequestParam String token,
                                                     @AuthenticationPrincipal CurrentUser principal) {
        InvitationResponse response = invitationUseCase.accept(token, principal.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> decline(@RequestParam String token,
                                                      @AuthenticationPrincipal CurrentUser principal) {
        InvitationResponse response = invitationUseCase.decline(token, principal.userId());
        return ResponseEntity.ok(response);
    }
}