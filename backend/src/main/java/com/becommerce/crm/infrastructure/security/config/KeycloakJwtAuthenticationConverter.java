package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.application.identity.port.input.AuthUseCase;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.identity.Permission;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.infrastructure.security.filter.CrmPrincipal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final AuthUseCase authUseCase;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public KeycloakJwtAuthenticationConverter(
            AuthUseCase authUseCase,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            PermissionRepository permissionRepository) {
        this.authUseCase = authUseCase;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        String keycloakSub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = jwt.getClaimAsString("name");

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

        List<String> realmRoles = extractRealmRoles(jwt);
        List<String> clientRoles = extractClientRoles(jwt);

        List<String> allRoles = new ArrayList<>(realmRoles);
        for (String clientRole : clientRoles) {
            if (!allRoles.contains(clientRole)) {
                allRoles.add(clientRole);
            }
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : allRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        User user;
        try {
            user = authUseCase.provisionKeycloakUser(
                    keycloakSub, email, preferredUsername, givenName, familyName);
        } catch (UserProvisioningException e) {
            throw new AuthenticationServiceException(e.getMessage());
        }
        UUID userId = user.getId();
        UUID companyId = user.getCompanyId();

        List<String> permissions = Collections.emptyList();

        List<String> roleNames = userRoleRepository.findByUserIdAndCompanyId(userId, companyId).stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        roleNames.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

        permissions = roleNames.stream()
                .flatMap(roleName -> {
                    try {
                        var roleNameEnum = com.becommerce.crm.domain.identity.valueobject.RoleName.valueOf(roleName);
                        Optional<Role> roleOpt = roleRepository.findByNameAndCompanyId(roleNameEnum, companyId);
                        return roleOpt.map(r -> rolePermissionRepository.findByRoleId(r.getId()).stream()
                                .map(rp -> permissionRepository.findById(rp.getPermissionId()))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .map(Permission::getName))
                                .orElse(java.util.stream.Stream.empty());
                    } catch (Exception e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .distinct()
                .collect(Collectors.toList());

        permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

        CrmPrincipal principal = CrmPrincipal.fromKeycloak(userId, companyId, keycloakSub, allRoles, permissions);

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        try {
            var realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map) {
                var roles = ((java.util.Map<String, Object>) realmAccess).get("roles");
                if (roles instanceof List) {
                    var roleList = (List<Object>) roles;
                    return roleList.stream()
                            .filter(Object.class::isInstance)
                            .map(Object::toString)
                            .map(String::toUpperCase)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractClientRoles(Jwt jwt) {
        List<String> result = new ArrayList<>();
        try {
            var resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess instanceof java.util.Map) {
                var resourceMap = (java.util.Map<String, Object>) resourceAccess;
                for (var entry : resourceMap.entrySet()) {
                    if (entry.getValue() instanceof java.util.Map) {
                        var roles = ((java.util.Map<String, Object>) entry.getValue()).get("roles");
                        if (roles instanceof List) {
                            for (Object role : (List<Object>) roles) {
                                result.add(role.toString().toUpperCase());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }
}
