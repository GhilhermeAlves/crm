package com.becommerce.crm.identity.application.dto;

import java.util.List;

public record CreateRoleRequest(
    String name,
    String description,
    List<String> permissionIds
) {}
