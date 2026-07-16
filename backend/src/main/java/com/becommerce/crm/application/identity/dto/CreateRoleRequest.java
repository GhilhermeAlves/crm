package com.becommerce.crm.application.identity.dto;

import java.util.List;

public record CreateRoleRequest(
    String name,
    String description,
    List<String> permissionIds
) {}
