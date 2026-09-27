package com.becommerce.crm.domain.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Item do catálogo da empresa (produto ou serviço), tabela {@code catalog_items} (V078).
 * Itens inativos continuam cadastrados, mas não são oferecidos ao cliente pelo agente.
 */
public class CatalogItem {

    private final UUID id;
    private final UUID companyId;
    private CatalogItemType type;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private String currency;
    private String sku;
    private boolean active;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private CatalogItem(UUID id, UUID companyId, CatalogItemType type, String name, String description,
                        String category, BigDecimal price, String currency, String sku, boolean active,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.active = active;
        apply(type, name, description, category, price, currency, sku);
    }

    public static CatalogItem create(UUID companyId, CatalogItemType type, String name, String description,
                                     String category, BigDecimal price, String currency, String sku) {
        LocalDateTime now = LocalDateTime.now();
        return new CatalogItem(UUID.randomUUID(), companyId, type, name, description, category, price,
                currency, sku, true, now, now);
    }

    public static CatalogItem reconstitute(UUID id, UUID companyId, CatalogItemType type, String name,
                                           String description, String category, BigDecimal price,
                                           String currency, String sku, boolean active,
                                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new CatalogItem(id, companyId, type, name, description, category, price, currency, sku,
                active, createdAt, updatedAt);
    }

    public void update(CatalogItemType type, String name, String description, String category,
                       BigDecimal price, String currency, String sku) {
        apply(type, name, description, category, price, currency, sku);
        touch();
    }

    public void activate() {
        this.active = true;
        touch();
    }

    public void deactivate() {
        this.active = false;
        touch();
    }

    private void apply(CatalogItemType type, String name, String description, String category,
                       BigDecimal price, String currency, String sku) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome do item é obrigatório");
        }
        if (price != null && price.signum() < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo");
        }
        this.type = Objects.requireNonNull(type, "type");
        this.name = name.trim();
        this.description = blankToNull(description);
        this.category = blankToNull(category);
        this.price = price;
        this.currency = currency == null || currency.isBlank() ? "BRL" : currency.trim().toUpperCase();
        this.sku = blankToNull(sku);
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public CatalogItemType getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public String getSku() { return sku; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
