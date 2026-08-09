package com.becommerce.crm.application.membership.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRoleRequest(
        @NotBlank(message = "role é obrigatória") String role) {
}
