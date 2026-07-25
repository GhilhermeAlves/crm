package com.becommerce.crm.domain.identity.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PasswordTest {

    @Test
    void shouldAcceptStrongPlainTextPasswordAndHideItWhenConvertedToString() {
        Password password = new Password("Strong1!");

        assertEquals("Strong1!", password.value());
        assertEquals("********", password.toString());
    }

    @Test
    void shouldAcceptBcryptHash() {
        Password password = Password.fromHash("$2a$12$abcdefghijklmnopqrstuu7mB5xQ6W1Qf4Q5H6Q7Q8Q9Q0Q1Q2Q3Q4Q5");

        assertEquals("********", password.toString());
    }

    @Test
    void shouldRejectNullBlankShortAndWeakPasswords() {
        assertThrows(IllegalArgumentException.class, () -> new Password(null));
        assertThrows(IllegalArgumentException.class, () -> new Password("   "));
        assertThrows(IllegalArgumentException.class, () -> new Password("Short1!"));
        assertThrows(IllegalArgumentException.class, () -> new Password("alllowercase1!"));
    }

    @Test
    void shouldRejectNullOrBlankHashes() {
        assertThrows(IllegalArgumentException.class, () -> Password.fromHash(null));
        assertThrows(IllegalArgumentException.class, () -> Password.fromHash(""));
    }
}
