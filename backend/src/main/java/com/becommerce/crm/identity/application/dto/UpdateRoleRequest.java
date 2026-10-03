package com.becommerce.crm.identity.application.dto;

import java.util.List;

public record UpdateRoleRequest(
    String description,
    Boolean isActive,
    List<String> permissionIds
) {}
