package com.becommerce.auth.application.identity.service;

import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.application.identity.port.output.PermissionRepository;
import com.becommerce.auth.application.identity.port.output.RoleRepository;
import com.becommerce.auth.application.identity.port.output.UserRepository;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.domain.identity.User;
import com.becommerce.auth.domain.identity.exception.UserInactiveException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolve o {@code CurrentUser} a partir da identidade autenticada do Keycloak.
 *
 * <p>Cadeia: identidade autenticada → usuário CRM (sub → e-mail) → rejeição de
 * inativo → empresa/tenant → roles → permissions → {@code CurrentUser}.
 *
 * <p>O provisionamento (criação de usuário inexistente) NÃO é duplicado aqui:
 * continua sendo responsabilidade do crm-backend (Sprint 1). Para identidade sem
 * usuário CRM, o resultado é o contrato {@code PROVISIONING_REQUIRED}, preparado
 * para receber o provisionamento quando esta responsabilidade migrar.
 */
@Service
public class CurrentUserResolutionService implements CurrentUserResolutionUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public CurrentUserResolutionService(UserRepository userRepository,
                                        RoleRepository roleRepository,
                                        PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResolution resolve(AuthenticatedIdentity identity) {
        Objects.requireNonNull(identity, "identity");

        User user = resolveUser(identity);
        if (user == null) {
            return new CurrentUserResolution.ProvisioningRequired(identity);
        }
        if (!user.active()) {
            throw new UserInactiveException("Usuário desativado: contate o administrador.");
        }

        List<String> roles = roleRepository.findRoleNamesByUserIdAndCompanyId(user.id(), user.companyId());
        List<String> permissions = permissionRepository.findPermissionNamesByUserIdAndCompanyId(user.id(), user.companyId());

        CurrentUser currentUser = new CurrentUser(
                user.id(),
                user.email(),
                user.companyId(),
                user.companyId(),
                roles,
                permissions,
                identity.keycloakSub(),
                identity.sessionId(),
                "keycloak",
                firstNonBlank(identity.displayName(), user.name()));

        return new CurrentUserResolution.Resolved(currentUser);
    }

    private User resolveUser(AuthenticatedIdentity identity) {
        if (identity.keycloakSub() != null && !identity.keycloakSub().isBlank()) {
            Optional<User> bySub = userRepository.findByKeycloakSub(identity.keycloakSub());
            if (bySub.isPresent()) {
                return bySub.get();
            }
        }
        if (identity.email() != null && !identity.email().isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(identity.email());
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }
        return null;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
