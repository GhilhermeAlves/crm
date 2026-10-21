package com.becommerce.crm.sales.chair.domain;

import com.becommerce.crm.sales.chair.domain.exception.ChairValidationException;

import java.time.Instant;
import java.util.UUID;

public class Chair {

    private static final int NAME_MAX_LENGTH = 120;

    private final UUID id;
    private final UUID companyId;
    private String name;
    private String color;
    private boolean active;
    private int sortOrder;
    private final Instant createdAt;
    private Instant updatedAt;

    private Chair(UUID id, UUID companyId, String name, String color, boolean active, int sortOrder,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.color = color;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Chair create(UUID companyId, String name, String color, boolean active, int sortOrder) {
        validateName(name);
        Instant now = Instant.now();
        return new Chair(UUID.randomUUID(), companyId, name.trim(), normalizeColor(color), active, sortOrder, now, now);
    }

    public static Chair reconstitute(UUID id, UUID companyId, String name, String color, boolean active,
                                     int sortOrder, Instant createdAt, Instant updatedAt) {
        return new Chair(id, companyId, name, color, active, sortOrder, createdAt, updatedAt);
    }

    public void update(String name, String color, Boolean active, Integer sortOrder) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (color != null) {
            this.color = normalizeColor(color);
        }
        if (active != null) {
            this.active = active;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        touch();
    }

    public void deactivate() {
        this.active = false;
        touch();
    }

    public void activate() {
        this.active = true;
        touch();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ChairValidationException("O nome da cadeira é obrigatório.");
        }
        if (name.trim().length() > NAME_MAX_LENGTH) {
            throw new ChairValidationException("O nome da cadeira deve ter no máximo " + NAME_MAX_LENGTH + " caracteres.");
        }
    }

    private static String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String trimmed = color.trim();
        if (!trimmed.matches("^#[0-9a-fA-F]{6}$")) {
            throw new ChairValidationException("A cor deve estar no formato hexadecimal (#RRGGBB).");
        }
        return trimmed.toUpperCase();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
