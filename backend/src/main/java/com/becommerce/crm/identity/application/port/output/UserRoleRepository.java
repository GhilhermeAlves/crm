package com.becommerce.crm.identity.application.port.output;

import com.becommerce.crm.identity.domain.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository {
    UserRole save(UserRole userRole);
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByCompanyId(UUID companyId);
    List<UserRole> findByUserIdAndCompanyId(UUID userId, UUID companyId);
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    void deleteByUserId(UUID userId);

    /**
     * True se o usuário é o dono do CRM (papel SUPER_ADMIN, V132) — independe da
     * empresa ativa: usa app.is_platform_admin_user(), que lê user_roles sem o
     * filtro de RLS por empresa.
     */
    boolean isPlatformAdmin(UUID userId);
}
