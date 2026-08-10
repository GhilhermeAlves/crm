package com.becommerce.crm.presentation.rest.onboarding;

import com.becommerce.crm.application.company.dto.CompanyResponse;
import com.becommerce.crm.application.company.dto.CreateCompanyRequest;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.onboarding.port.input.OnboardingUseCase;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.exception.UserNotFoundException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Onboarding self-service (Sprint 8.3).
 *
 * <p>Endpoint autenticado (qualquer usuário com sessão), sem exigir
 * {@code company:create}: o gate de acesso ao CRM é contornado para usuários
 * SEM empresa (onboarding pendente) — eles chegam aqui antes de ter
 * permissions/roles. O uso indevido é mo perceptível na UI porque a empresa é
 * criada PARA o próprio usuário (vira OWNER dela).
 */
@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private final OnboardingUseCase onboardingUseCase;
    private final UserRepository userRepository;

    public OnboardingController(OnboardingUseCase onboardingUseCase, UserRepository userRepository) {
        this.onboardingUseCase = onboardingUseCase;
        this.userRepository = userRepository;
    }

    @PostMapping("/companies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponse> onboard(@Valid @RequestBody CreateCompanyRequest request,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        User owner = userRepository.findById(principal.userId())
                .orElseThrow(UserNotFoundException::new);
        if (owner.getCompanyId() != null) {
            throw new IllegalStateException(
                    "Usuário já possui empresa; o onboarding destina-se apenas a usuários sem empresa.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(onboardingUseCase.onboard(request, owner));
    }
}