package com.becommerce.crm.identity.invitation.web;

import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.AcceptRequest;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.CreateRequest;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.LinkResponse;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.PreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.RegisterRequest;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.Response;
import com.becommerce.crm.identity.invitation.application.service.CompanyInvitationService;
import com.becommerce.crm.identity.invitation.application.service.CompanyInvitationSignupService;
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
 * Convite de criação de empresa (V123). Administração exclusiva do CRM_ADMIN;
 * prévia e cadastro são públicos (token) e o aceite exige login com o e-mail
 * do convite.
 */
@RestController
@RequestMapping("/api/v1/company-invitations")
public class CompanyInvitationController {

    private final CompanyInvitationService invitationService;
    private final CompanyInvitationSignupService signupService;

    public CompanyInvitationController(CompanyInvitationService invitationService,
                                       CompanyInvitationSignupService signupService) {
        this.invitationService = invitationService;
        this.signupService = signupService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CRM_ADMIN')")
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateRequest request,
                                               @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.create(principal.companyId(), request, principal.userId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('CRM_ADMIN')")
    public ResponseEntity<List<Response>> list(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(invitationService.list(principal.companyId()));
    }

    @DeleteMapping("/{invitationId}")
    @PreAuthorize("hasRole('CRM_ADMIN')")
    public ResponseEntity<Void> revoke(@PathVariable UUID invitationId,
                                       @AuthenticationPrincipal CurrentUser principal) {
        invitationService.revoke(principal.companyId(), invitationId);
        return ResponseEntity.noContent().build();
    }

    /** Novo link (o anterior deixa de valer). {@code send=true}: reenviar por e-mail. */
    @PostMapping("/{invitationId}/regenerate")
    @PreAuthorize("hasRole('CRM_ADMIN')")
    public ResponseEntity<LinkResponse> regenerate(@PathVariable UUID invitationId,
                                                   @RequestParam(defaultValue = "false") boolean send,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(invitationService.regenerate(principal.companyId(), invitationId, send));
    }

    /** Público: prévia em /convite-empresa/{token}. */
    @GetMapping("/preview")
    public ResponseEntity<PreviewResponse> preview(
            @RequestParam String token,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        return ResponseEntity.ok(signupService.preview(token, clientKey(realIp)));
    }

    /** Público: cria a conta e a empresa de quem ainda não tem conta. */
    @PostMapping("/register")
    public ResponseEntity<Response> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signupService.register(request, clientKey(realIp)));
    }

    @PostMapping("/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> accept(@Valid @RequestBody AcceptRequest request,
                                           @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(invitationService.accept(request.token(), request.company(), principal.userId()));
    }

    private static String clientKey(String realIp) {
        return realIp != null && !realIp.isBlank() ? realIp : "unknown";
    }
}
