package com.becommerce.crm.presentation.rest.notification;

import com.becommerce.crm.application.notification.dto.CreateNotificationRequest;
import com.becommerce.crm.application.notification.dto.NotificationResponse;
import com.becommerce.crm.application.notification.port.input.NotificationUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Notificações in-app (módulo de Notificações). As notificações são pessoais:
 * a listagem, a contagem e a marcação de leitura operam sobre as notificações do
 * usuário autenticado ({@code principal.userId()}) dentro da empresa ativa.
 */
@RestController
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    public NotificationController(NotificationUseCase notificationUseCase) {
        this.notificationUseCase = notificationUseCase;
    }

    @GetMapping("/api/v1/companies/{companyId}/notifications")
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseEntity<List<NotificationResponse>> listMine(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(notificationUseCase.listMine(companyId, principal.userId()));
    }

    @GetMapping("/api/v1/companies/{companyId}/notifications/unread-count")
    @PreAuthorize("hasAuthority('notification:read')")
    public ResponseEntity<Long> countUnread(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(notificationUseCase.countUnread(companyId, principal.userId()));
    }

    @PostMapping("/api/v1/companies/{companyId}/notifications/{notificationId}/read")
    @PreAuthorize("hasAuthority('notification:update')")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID companyId,
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(notificationUseCase.markAsRead(companyId, notificationId, principal.userId()));
    }

    @PostMapping("/api/v1/companies/{companyId}/notifications/read-all")
    @PreAuthorize("hasAuthority('notification:update')")
    public ResponseEntity<Void> markAllRead(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        notificationUseCase.markAllRead(companyId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/companies/{companyId}/notifications")
    @PreAuthorize("hasAuthority('notification:create')")
    public ResponseEntity<NotificationResponse> create(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateNotificationRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        NotificationResponse response = notificationUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar notificações da sua própria empresa.");
        }
    }
}