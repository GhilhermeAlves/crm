package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.presentation.rest.dto.ResolutionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * API interna de resolução do usuário autenticado. A identidade é sempre
 * derivada do JWT do Keycloak (contexto autenticado) — o endpoint não aceita
 * {@code userId}/{@code companyId}/{@code roles}/{@code permissions} como entrada.
 */
@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final CurrentUserResolutionUseCase currentUserResolutionUseCase;

    public InternalAuthController(CurrentUserResolutionUseCase currentUserResolutionUseCase) {
        this.currentUserResolutionUseCase = currentUserResolutionUseCase;
    }

    @GetMapping("/current-user")
    public ResponseEntity<ResolutionResponse> currentUser(@AuthenticationPrincipal AuthenticatedIdentity identity) {
        Objects.requireNonNull(identity, "identidade autenticada obrigatória");
        CurrentUserResolution resolution = currentUserResolutionUseCase.resolve(identity);
        return ResponseEntity.ok(ResolutionResponse.from(resolution));
    }
}
