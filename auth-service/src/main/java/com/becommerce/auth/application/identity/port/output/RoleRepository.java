package com.becommerce.auth.application.identity.port.output;

import java.util.List;
import java.util.UUID;

/**
 * Porta de saída para resolução de roles do CRM (tabelas {@code roles} e
 * {@code user_roles}).
 */
public interface RoleRepository {

    List<String> findRoleNamesByUserIdAndCompanyId(UUID userId, UUID companyId);
}
