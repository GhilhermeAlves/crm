package com.becommerce.crm.shared.security.config;

import com.becommerce.crm.identity.application.port.input.AuthUseCase;
import com.becommerce.crm.identity.application.port.output.PermissionRepository;
import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.identity.infrastructure.client.AuthServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seleciona a implementação de {@link CurrentUserResolver} pela flag
 * {@code app.auth.identity-layer.enabled} (rollback por serviço):
 *
 * <ul>
 *   <li>{@code false} (default) — resolução local no próprio serviço;</li>
 *   <li>{@code true} — consumo do crm-auth-service como camada de identidade,
 *       com fallback local.</li>
 * </ul>
 */
@Configuration
public class CurrentUserResolverConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.auth.identity-layer.enabled", havingValue = "true")
    public CurrentUserResolver authServiceCurrentUserResolver(
            AuthServiceClient authServiceClient,
            AuthUseCase authUseCase,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            MembershipRepository membershipRepository) {
        LocalCurrentUserResolver localResolver = new LocalCurrentUserResolver(
                authUseCase, userRoleRepository, roleRepository, permissionRepository, membershipRepository);
        return new AuthServiceCurrentUserResolver(authServiceClient, localResolver);
    }

    @Bean
    @ConditionalOnProperty(name = "app.auth.identity-layer.enabled", havingValue = "false", matchIfMissing = true)
    public CurrentUserResolver localCurrentUserResolver(
            AuthUseCase authUseCase,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            MembershipRepository membershipRepository) {
        return new LocalCurrentUserResolver(
                authUseCase, userRoleRepository, roleRepository, permissionRepository, membershipRepository);
    }
}
