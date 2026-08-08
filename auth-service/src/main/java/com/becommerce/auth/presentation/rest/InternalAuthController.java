package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.application.gateway.port.output.CredentialResetClient;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.presentation.rest.dto.ResetCredentialRequest;
import com.becommerce.auth.presentation.rest.dto.ResolutionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * API interna de resolução do usuário autenticado e de credenciais. A identidade
 * é sempre derivada do JWT do Keycloak (contexto autenticado) — o endpoint não aceita
 * {@code userId}/{@code companyId}/{@code roles}/{@code permissions} como entrada.
 * O {@code POST /internal/auth/reset-password} (Sprint 7.4) é o único sem JWT de
 * usuário: autenticado por segredo de serviço (ver {@code InternalApiTokenFilter}).
 */
@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final CurrentUserResolutionUseCase currentUserResolutionUseCase;
    private final CredentialResetClient credentialResetClient;

    public InternalAuthController(CurrentUserResolutionUseCase currentUserResolutionUseCase,
                                  CredentialResetClient credentialResetClient) {
        this.currentUserResolutionUseCase = currentUserResolutionUseCase;
        this.credentialResetClient = credentialResetClient;
    }

    @GetMapping("/current-user")
    public ResponseEntity<ResolutionResponse> currentUser(@AuthenticationPrincipal AuthenticatedIdentity identity) {
        Objects.requireNonNull(identity, "identidade autenticada obrigatória");
        CurrentUserResolution resolution = currentUserResolutionUseCase.resolve(identity);
        return ResponseEntity.ok(ResolutionResponse.from(resolution));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetCredentialRequest request) {
        credentialResetClient.resetPassword(request.keycloakSub(), request.email(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
