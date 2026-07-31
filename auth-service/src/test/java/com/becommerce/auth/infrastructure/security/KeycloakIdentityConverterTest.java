package com.becommerce.auth.infrastructure.security;

import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class KeycloakIdentityConverterTest {

    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    private final KeycloakIdentityConverter converter = new KeycloakIdentityConverter();

    @Test
    void shouldExtractAuthenticatedIdentityFromJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://srv1348261.hstgr.cloud/realms/CRM")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("preferred_username", EMAIL)
                .claim("given_name", "Ghilherme")
                .claim("family_name", "Santos")
                .claim("name", "Ghilherme Santos")
                .claim("sid", "session-9")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertInstanceOf(AuthenticatedIdentity.class, authentication.getPrincipal());
        AuthenticatedIdentity identity = (AuthenticatedIdentity) authentication.getPrincipal();
        assertEquals(SUB, identity.keycloakSub());
        assertEquals(EMAIL, identity.email());
        assertEquals(EMAIL, identity.preferredUsername());
        assertEquals("Ghilherme", identity.givenName());
        assertEquals("Santos", identity.familyName());
        assertEquals("Ghilherme Santos", identity.displayName());
        assertEquals("session-9", identity.sessionId());
    }

    @Test
    void shouldDeriveDisplayNameFromGivenAndFamilyWhenNameMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://srv1348261.hstgr.cloud/realms/CRM")
                .subject(SUB)
                .claim("given_name", "Ghilherme")
                .claim("family_name", "Santos")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AuthenticatedIdentity identity = (AuthenticatedIdentity) converter.convert(jwt).getPrincipal();

        assertEquals("Ghilherme Santos", identity.displayName());
        assertNull(identity.sessionId());
    }
}
