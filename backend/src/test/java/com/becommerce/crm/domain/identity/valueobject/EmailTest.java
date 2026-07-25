package com.becommerce.crm.domain.identity.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void shouldExposeValuePartsAndStringRepresentation() {
        Email email = new Email("user@example.com");

        assertEquals("user@example.com", email.value());
        assertEquals("user", email.getLocalPart());
        assertEquals("example.com", email.getDomain());
        assertEquals("user@example.com", email.toString());
    }

    @Test
    void shouldRejectNullAndBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
        assertThrows(IllegalArgumentException.class, () -> new Email("   "));
    }

    @Test
    void shouldRejectInvalidEmailFormats() {
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid-email"));
        assertThrows(IllegalArgumentException.class, () -> new Email("user@@example.com"));
    }
}
