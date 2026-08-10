package com.becommerce.crm.application.me.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Corpo de {@code POST /api/v1/me/switch-company}.
 */
public record SwitchCompanyRequest(
        @NotNull(message = "companyId é obrigatório")
        UUID companyId) {
}