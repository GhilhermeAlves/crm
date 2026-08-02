package com.becommerce.auth.application.identity.service;

import com.becommerce.auth.application.company.port.output.CompanyRepository;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.application.identity.port.output.PermissionRepository;
import com.becommerce.auth.application.identity.port.output.RoleRepository;
import com.becommerce.auth.application.identity.port.output.UserRepository;
import com.becommerce.auth.domain.company.CompanyStatus;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.domain.identity.User;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.auth.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolve o {@code CurrentUser} a partir da identidade autenticada do Keycloak.
 *
 * <p>Cadeia: identidade autenticada → usuário CRM (sub → e-mail) → gate de
 * acesso ao CRM (Sprint 6) → empresa/tenant → roles → permissions →
 * {@code CurrentUser}.
 *
 * <p>Gate de acesso (paridade com o crm-backend, Sprint 6):
 *
 * <pre>
 *   users.is_active = true
 *   AND users.crm_enabled = true
 *   AND companies.status = ACTIVE
 * </pre>
 *
 * Qualquer falha → {@link CrmAccessDeniedException} ({@code 403 CRM_ACCESS_DENIED}).
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
    private final CompanyRepository companyRepository;

    public CurrentUserResolutionService(UserRepository userRepository,
                                        RoleRepository roleRepository,
                                        PermissionRepository permissionRepository,
                                        CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public CurrentUserResolution resolve(AuthenticatedIdentity identity) {
        Objects.requireNonNull(identity, "identity");

        // Bootstrap de identidade sob RLS FORCE: o sub do JWT permite ler a
        // própria linha em users (V022) antes do company_id ser conhecido. Sem
        // transação por requisição: cada consulta obtém uma conexão própria e o
        // TenantAwareDataSource aplica o GUC vigente no momento, incluindo o
        // company_id assim que ele é resolvido.
        TenantContext.setKeycloakSub(identity.keycloakSub());
        try {
            User user = resolveUser(identity);
            if (user == null) {
                return new CurrentUserResolution.ProvisioningRequired(identity);
            }
            assertCrmAccess(user);

            TenantContext.setCompanyId(user.companyId());

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
        } finally {
            TenantContext.clear();
        }
    }

    private void assertCrmAccess(User user) {
        if (!user.active()) {
            throw new CrmAccessDeniedException("Usuário inativo: acesso ao CRM negado.");
        }
        if (!user.crmEnabled()) {
            throw new CrmAccessDeniedException(
                "Usuário sem acesso ao CRM (crm_enabled=false): conceda acesso explicitamente.");
        }
        CompanyStatus status = companyRepository.findStatusById(user.companyId())
            .orElseThrow(() -> new CrmAccessDeniedException(
                "Empresa não encontrada: acesso ao CRM negado."));
        if (!status.canOperate()) {
            throw new CrmAccessDeniedException(
                "Empresa " + status + ": acesso ao CRM negado.");
        }
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
