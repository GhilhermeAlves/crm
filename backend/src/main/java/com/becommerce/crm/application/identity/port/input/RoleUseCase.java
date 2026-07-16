package com.becommerce.crm.application.identity.port.input;

import com.becommerce.crm.application.identity.dto.*;

import java.util.List;
import java.util.UUID;

public interface RoleUseCase {
    List<RoleResponse> listRoles(UUID companyId);
    RoleResponse getRoleById(UUID id);
    RoleResponse createRole(UUID companyId, CreateRoleRequest request);
    RoleResponse updateRole(UUID id, UpdateRoleRequest request);
    void deleteRole(UUID id);
    void assignPermissionToRole(UUID roleId, String permissionId);
    void removePermissionFromRole(UUID roleId, String permissionId);
    void assignRoleToUser(UUID userId, UUID roleId, UUID companyId);
    void removeRoleFromUser(UUID userId, UUID roleId);
    List<RoleResponse> getUserRoles(UUID userId, UUID companyId);
}
