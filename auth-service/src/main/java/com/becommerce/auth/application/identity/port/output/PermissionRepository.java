package com.becommerce.auth.application.identity.port.output;

import java.util.List;
import java.util.UUID;

/**
 * Porta de saída para resolução de permissions do CRM (tabelas {@code permissions},
 * {@code role_permissions} e {@code user_roles}).
 */
public interface PermissionRepository {

    List<String> findPermissionNamesByUserIdAndCompanyId(UUID userId, UUID companyId);
}
