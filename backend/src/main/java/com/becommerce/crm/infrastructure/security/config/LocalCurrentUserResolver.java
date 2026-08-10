package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.application.identity.port.input.AuthUseCase;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.identity.Permission;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.identity.exception.LinkingRequiredException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolução local do {@link CurrentUser} a partir do banco CRM: provisiona o
 * usuário Keycloak (Sprint 1) e resolve roles/permissions do RBAC. Usada por
 * padrão (flag {@code app.auth.identity-layer.enabled=false}) e como fallback
 * da resolução via crm-auth-service.
 */
public class LocalCurrentUserResolver implements CurrentUserResolver {

    private final AuthUseCase authUseCase;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final MembershipRepository membershipRepository;

    public LocalCurrentUserResolver(
            AuthUseCase authUseCase,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            PermissionRepository permissionRepository,
            MembershipRepository membershipRepository) {
        this.authUseCase = authUseCase;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public CurrentUser resolve(Jwt jwt) {
        String keycloakSub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = jwt.getClaimAsString("name");
        String identityProvider = jwt.getClaimAsString("identity_provider");

        if (givenName == null && familyName == null && name != null && !name.isBlank()) {
            List<String> parts = Arrays.stream(name.trim().split("\\s+"))
                    .filter(part -> !part.isBlank())
                    .collect(Collectors.toList());
            if (!parts.isEmpty()) {
                givenName = parts.get(0);
                if (parts.size() > 1) {
                    familyName = String.join(" ", parts.subList(1, parts.size()));
                }
            }
        }

        // Bootstrap de identidade sob RLS FORCE: antes de conhecer o company_id,
        // o sub do JWT permite ler a própria linha em users (V022) e o e-mail
        // permite ler/vincular a própria linha por e-mail (V024). Definidos aqui
        // para a transação de provisionamento; o company_id entra logo em seguida,
        // quando a consulta de RBAC precisa do tenant.
        TenantContext.setKeycloakSub(keycloakSub);
        TenantContext.setIdentityEmail(email);
        try {
            User user;
            try {
                user = authUseCase.provisionKeycloakUser(
                        keycloakSub, email, preferredUsername, givenName, familyName, identityProvider);
            } catch (UserProvisioningException e) {
                throw new AuthenticationServiceException(e.getMessage());
            } catch (LinkingRequiredException e) {
                throw new LinkingRequiredAuthenticationException(e.getMessage());
            } catch (CrmAccessDeniedException e) {
                throw new CrmAccessDeniedAuthenticationException(e.getMessage());
            }

            UUID userId = user.getId();
            UUID companyId = user.getCompanyId();

            // Sprint 8.3: usuário provisionado SEM empresa (onboarding pendente).
            // Autenticado, porém SEM company_id / roles / permissions — o gate da
            // UI redireciona para a tela de onboarding ("crie sua empresa") e os
            // módulos CRM exigem company_id (ver CompanyRequiredRoute no frontend
            // e os gates de autorização no backend).
            if (companyId == null) {
                return CurrentUser.fromKeycloak(userId, email, null, List.of(), List.of(),
                        keycloakSub, buildDisplayName(name, givenName, familyName, email), null);
            }
            TenantContext.setCompanyId(companyId);

            // Sprint 8.2: membro desligado (sem membership ACTIVE) perde acesso.
            if (!membershipRepository.existsActiveByUserIdAndCompanyId(userId, companyId)) {
                throw new CrmAccessDeniedException(
                        "Usuário sem membership ativa nesta empresa: acesso ao CRM negado.");
            }

            List<String> roleNames = userRoleRepository.findByUserIdAndCompanyId(userId, companyId).stream()
                    .map(ur -> roleRepository.findById(ur.getRoleId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            List<String> permissions = roleNames.stream()
                    .flatMap(roleName -> permissionsForRole(roleName, companyId))
                    .distinct()
                    .collect(Collectors.toList());

            String membershipRole = membershipRepository
                    .findMembershipRoleByUserIdAndCompanyId(userId, companyId)
                    .orElse(null);

            String displayName = buildDisplayName(name, givenName, familyName, email);

            return CurrentUser.fromKeycloak(userId, email, companyId, roleNames, permissions, keycloakSub, displayName, membershipRole);
        } finally {
            TenantContext.clear();
        }
    }

    private Stream<String> permissionsForRole(String roleName, UUID companyId) {
        try {
            Optional<Role> roleOpt = roleRepository.findByNameAndCompanyId(RoleName.valueOf(roleName), companyId);
            return roleOpt.map(role -> rolePermissionRepository.findByRoleId(role.getId()).stream()
                            .map(rp -> permissionRepository.findById(rp.getPermissionId()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(Permission::getName))
                    .orElse(Stream.empty());
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    private String buildDisplayName(String name, String givenName, String familyName, String email) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (givenName != null && !givenName.isBlank()) {
            return familyName != null && !familyName.isBlank()
                    ? givenName + " " + familyName
                    : givenName;
        }
        return email;
    }
}
