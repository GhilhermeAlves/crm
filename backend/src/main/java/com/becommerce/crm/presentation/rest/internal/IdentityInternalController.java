package com.becommerce.crm.presentation.rest.internal;

import com.becommerce.crm.application.identity.service.AuthService;
import com.becommerce.crm.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.crm.domain.identity.exception.LinkingRequiredException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

/**
 * API interna de identidade (Sprint 7.2 — Account Linking), consumida pelo
 * crm-auth-service na rede interna. A identidade é SEMPRE derivada do JWT do
 * Keycloak (contexto autenticado) — o corpo só carrega a senha de verificação.
 *
 * <ul>
 *   <li>{@code POST /internal/auth/provision} — Caso C: provisiona a identidade
 *       externa (reusa {@code provisionKeycloakUser}); e-mail já existente com
 *       conta local → {@code 409 LINKING_REQUIRED} (nunca auto-vincula).</li>
 *   <li>{@code POST /internal/auth/link} — Caso B: verifica a senha da conta
 *       local por e-mail e vincula o {@code keycloak_sub}. Idempotente.</li>
 * </ul>
 *
 * <p>Provedor externo (claim {@code identity_provider}) exige
 * {@code email_verified=true}: identidade com e-mail não verificado não
 * provisiona nem vincula.
 */
@RestController
@RequestMapping("/internal/auth")
public class IdentityInternalController {

    private final AuthService authService;

    public IdentityInternalController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/provision")
    public ResponseEntity<InternalIdentityResponse> provision(@AuthenticationPrincipal Jwt jwt) {
        Objects.requireNonNull(jwt, "identidade autenticada obrigatória");
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String identityProvider = jwt.getClaimAsString("identity_provider");

        requireVerifiedEmail(jwt, identityProvider);

        TenantContext.setKeycloakSub(sub);
        TenantContext.setIdentityEmail(email);
        try {
            authService.provisionKeycloakUser(sub, email, preferredUsername, givenName, familyName, identityProvider);
            return ResponseEntity.ok(InternalIdentityResponse.provisioned(email));
        } catch (LinkingRequiredException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(InternalIdentityResponse.linkingRequired(email));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/link")
    public ResponseEntity<InternalIdentityResponse> link(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestBody LinkRequest body) {
        Objects.requireNonNull(jwt, "identidade autenticada obrigatória");
        Objects.requireNonNull(body, "body obrigatório");

        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        requireVerifiedEmail(jwt, jwt.getClaimAsString("identity_provider"));

        try {
            authService.linkKeycloakIdentity(sub, email, givenName, familyName, body.password());
            return ResponseEntity.ok(InternalIdentityResponse.linked(email));
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(InternalIdentityResponse.invalidCredentials(email));
        } catch (UserProvisioningException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(InternalIdentityResponse.linkNotFound(email));
        }
    }

    /**
     * Provedor externo (ex.: Google) só provisiona/víncula com e-mail verificado
     * pelo provedor ({@code email_verified=true} no JWT).
     */
    private void requireVerifiedEmail(Jwt jwt, String identityProvider) {
        if (isExternalProvider(identityProvider)
                && !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new UserProvisioningException(
                "E-mail não verificado pelo provedor de identidade: provisionamento/vínculo negado.");
        }
    }

    private boolean isExternalProvider(String provider) {
        return provider != null && !provider.isBlank() && !"keycloak".equalsIgnoreCase(provider);
    }

    public record LinkRequest(String password) {
        public LinkRequest {
            java.util.Objects.requireNonNull(password, "password");
        }
    }

    /**
     * Resposta discriminada dos endpoints internos de identidade:
     * {@code PROVISIONED}/{@code LINKED}/{@code LINKING_REQUIRED}/
     * {@code INVALID_CREDENTIALS}/{@code LINK_NOT_FOUND}.
     */
    public record InternalIdentityResponse(String status, String email) {
        static InternalIdentityResponse provisioned(String email) {
            return new InternalIdentityResponse("PROVISIONED", email);
        }

        static InternalIdentityResponse linked(String email) {
            return new InternalIdentityResponse("LINKED", email);
        }

        static InternalIdentityResponse linkingRequired(String email) {
            return new InternalIdentityResponse("LINKING_REQUIRED", email);
        }

        static InternalIdentityResponse invalidCredentials(String email) {
            return new InternalIdentityResponse("INVALID_CREDENTIALS", email);
        }

        static InternalIdentityResponse linkNotFound(String email) {
            return new InternalIdentityResponse("LINK_NOT_FOUND", email);
        }
    }
}
