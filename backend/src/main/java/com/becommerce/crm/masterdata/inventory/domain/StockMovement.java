package com.becommerce.crm.masterdata.inventory.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Movimentação de estoque de um item de produto ({@code inventory_movements}, V095).
 * {@code delta} é a variação aplicada no saldo (com sinal): positivo em ENTRADA,
 * negativo em SAIDA e a diferença em AJUSTE.
 */
public class StockMovement {

    private final UUID id;
    private final UUID companyId;
    private final UUID catalogItemId;
    private StockMovementType type;
    private int delta;
    private String note;
    private final LocalDateTime createdAt;

    private StockMovement(UUID id, UUID companyId, UUID catalogItemId, StockMovementType type,
                          int delta, String note, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId");
        this.createdAt = createdAt;
        this.type = Objects.requireNonNull(type, "type");
        this.delta = delta;
        this.note = note == null || note.isBlank() ? null : note.trim();
    }

    public static StockMovement create(UUID companyId, UUID catalogItemId, StockMovementType type,
                                       int delta, String note) {
        return new StockMovement(UUID.randomUUID(), companyId, catalogItemId, type, delta, note,
                LocalDateTime.now());
    }

    public static StockMovement reconstitute(UUID id, UUID companyId, UUID catalogItemId,
                                             StockMovementType type, int delta, String note,
                                             LocalDateTime createdAt) {
        return new StockMovement(id, companyId, catalogItemId, type, delta, note, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getCatalogItemId() { return catalogItemId; }
    public StockMovementType getType() { return type; }
    public int getDelta() { return delta; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
