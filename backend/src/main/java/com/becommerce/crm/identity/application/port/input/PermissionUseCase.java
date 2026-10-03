package com.becommerce.crm.identity.application.port.input;

import com.becommerce.crm.identity.application.dto.PermissionResponse;

import java.util.List;

public interface PermissionUseCase {
    List<PermissionResponse> listAllPermissions();
    List<PermissionResponse> listPermissionsByModule(String module);
    PermissionResponse getPermissionById(String id);
}
