package com.becommerce.crm.masterdata.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovementJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "catalog_item_id")
    private UUID catalogItemId;

    @Column(name = "movement_type")
    private String movementType;

    @Column(name = "delta")
    private int delta;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getCatalogItemId() { return catalogItemId; }
    public void setCatalogItemId(UUID catalogItemId) { this.catalogItemId = catalogItemId; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
