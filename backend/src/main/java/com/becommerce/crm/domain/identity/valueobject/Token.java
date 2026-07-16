package com.becommerce.crm.domain.identity.valueobject;

import java.util.UUID;

public record Token(String value, UUID userId, UUID companyId, String family) {
    public Token {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }
    }
}
