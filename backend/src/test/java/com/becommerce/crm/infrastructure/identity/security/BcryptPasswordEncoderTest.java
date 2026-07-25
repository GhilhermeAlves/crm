package com.becommerce.crm.infrastructure.identity.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BcryptPasswordEncoderTest {

    private final BcryptPasswordEncoder encoder = new BcryptPasswordEncoder();

    @Test
    void shouldEncodePasswordAndMatchOnlyTheOriginalValue() {
        String encodedPassword = encoder.encode("Strong1!");

        assertTrue(encodedPassword.startsWith("$2"));
        assertTrue(encoder.matches("Strong1!", encodedPassword));
        assertFalse(encoder.matches("Wrong1!", encodedPassword));
    }
}
