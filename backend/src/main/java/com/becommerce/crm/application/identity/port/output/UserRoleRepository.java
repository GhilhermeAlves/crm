package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.UserRole;

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
}
