package com.becommerce.auth.application.identity.port.output;

import java.util.List;
import java.util.UUID;

/**
 * Porta de saída para resolução de roles do CRM (tabelas {@code roles} e
 * {@code user_roles}).
 */
public interface RoleRepository {

    List<String> findRoleNamesByUserIdAndCompanyId(UUID userId, UUID companyId);

    /**
     * True se o usuário é o dono do CRM (papel SUPER_ADMIN, V132), em qualquer
     * empresa: usa {@code app.is_platform_admin_user()}, que lê user_roles sem o
     * filtro de RLS da empresa ativa.
     */
    boolean isPlatformAdmin(UUID userId);
}
