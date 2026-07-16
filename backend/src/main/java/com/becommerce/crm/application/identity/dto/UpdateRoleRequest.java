package com.becommerce.crm.application.identity.dto;

import java.util.List;

public record UpdateRoleRequest(
    String description,
    Boolean isActive,
    List<String> permissionIds
) {}
