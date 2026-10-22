package com.becommerce.crm.sales.tag.domain;

import com.becommerce.crm.sales.tag.domain.exception.TagValidationException;

import java.time.Instant;
import java.util.UUID;

public class Tag {

    private static final int NAME_MAX_LENGTH = 120;

    private final UUID id;
    private final UUID companyId;
    private String name;
    private String color;
    private final Instant createdAt;
    private Instant updatedAt;

    private Tag(UUID id, UUID companyId, String name, String color, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Tag create(UUID companyId, String name, String color) {
        validateName(name);
        Instant now = Instant.now();
        return new Tag(UUID.randomUUID(), companyId, name.trim(), normalizeColor(color), now, now);
    }

    public static Tag reconstitute(UUID id, UUID companyId, String name, String color, Instant createdAt,
                                   Instant updatedAt) {
        return new Tag(id, companyId, name, color, createdAt, updatedAt);
    }

    public void update(String name, String color) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (color == null) {
            this.color = null;
        } else {
            this.color = normalizeColor(color);
        }
        touch();
    }

    public void setColor(String color) {
        if (color == null) {
            this.color = null;
        } else {
            this.color = normalizeColor(color);
        }
        touch();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new TagValidationException("O nome do marcador é obrigatório.");
        }
        if (name.trim().length() > NAME_MAX_LENGTH) {
            throw new TagValidationException("O nome do marcador deve ter no máximo " + NAME_MAX_LENGTH + " caracteres.");
        }
    }

    private static String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String trimmed = color.trim();
        if (!trimmed.matches("^#[0-9a-fA-F]{6}$")) {
            throw new TagValidationException("A cor deve estar no formato hexadecimal (#RRGGBB).");
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
