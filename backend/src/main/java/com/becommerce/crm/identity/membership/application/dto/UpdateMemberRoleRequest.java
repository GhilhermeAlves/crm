package com.becommerce.crm.identity.membership.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRoleRequest(
        @NotBlank(message = "role é obrigatória") String role) {
}
