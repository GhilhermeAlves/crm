package com.becommerce.crm.application.identity.port.input;

import com.becommerce.crm.application.identity.dto.PermissionResponse;

import java.util.List;

public interface PermissionUseCase {
    List<PermissionResponse> listAllPermissions();
    List<PermissionResponse> listPermissionsByModule(String module);
    PermissionResponse getPermissionById(String id);
}
