package com.becommerce.crm.masterdata.inventory.application.dto;

import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro de movimento. Em {@code AJUSTE}, {@code quantity} é a contagem real;
 * o delta é calculado no service.
 */
public record RegisterMovementRequest(
        @NotNull StockMovementType type,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 500) String note
) {
}
