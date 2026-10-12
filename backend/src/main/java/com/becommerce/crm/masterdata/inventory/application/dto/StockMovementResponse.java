package com.becommerce.crm.masterdata.inventory.application.dto;

import com.becommerce.crm.masterdata.inventory.domain.StockMovement;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID catalogItemId,
        StockMovementType type,
        int delta,
        String note,
        Integer balanceAfter,
        LocalDateTime createdAt
) {
    /** {@code balanceAfter} é {@code null} na listagem do histórico (só o registro conhece o saldo). */
    public static StockMovementResponse from(StockMovement m, Integer balanceAfter) {
        return new StockMovementResponse(m.getId(), m.getCatalogItemId(), m.getType(), m.getDelta(),
                m.getNote(), balanceAfter, m.getCreatedAt());
    }
}
