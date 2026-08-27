package com.becommerce.crm.domain.identity.valueobject;

import java.util.regex.Pattern;

public record Password(String value) {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$"
    );

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$");

    public Password {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        if (!BCRYPT_PATTERN.matcher(value).find()) {
            if (value.length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
            if (!PASSWORD_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException(
                    "Password must contain at least 1 uppercase, 1 lowercase, 1 number, and 1 symbol"
                );
            }
        }
    }

    public static Password fromHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }
        return new Password(hash);
    }

    @Override
    public String toString() {
        return "********";
    }
}
