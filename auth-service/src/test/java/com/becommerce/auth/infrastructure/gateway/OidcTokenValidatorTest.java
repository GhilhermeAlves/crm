package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcTokenValidatorTest {

    private static final String ISSUER = "https://srv1348261.hstgr.cloud/realms/CRM";
    private static final String CLIENT_ID = "crm-gateway";

    @Mock private JwtDecoder jwtDecoder;

    private OidcGatewayProperties properties;
    private OidcTokenValidator validator;

    @BeforeEach
    void setUp() {
        properties = new OidcGatewayProperties();
        properties.setIssuerUri(ISSUER);
        properties.setClientId(CLIENT_ID);
        validator = new OidcTokenValidator(jwtDecoder, properties);
    }

    private Jwt idToken(String nonce, String issuer, List<String> aud, String azp,
                        Instant issuedAt, Instant expiresAt) {
        Jwt.Builder builder = Jwt.withTokenValue("id-token")
                .header("alg", "RS256")
                .subject("sub-1")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("aud", aud)
                .claim("azp", azp)
                .claim("nonce", nonce);
        if (issuer != null) {
            builder.issuer(issuer);
        }
        return builder.build();
    }

    private Jwt validIdToken() {
        Instant now = Instant.now();
        return idToken("nonce-1", ISSUER, List.of(CLIENT_ID), CLIENT_ID,
                now.minusSeconds(60), now.plusSeconds(3600));
    }

    @Test
    void shouldAcceptValidIdToken() {
        Jwt jwt = validIdToken();
        when(jwtDecoder.decode("id-token")).thenReturn(jwt);

        Jwt validated = validator.validateIdToken("id-token", "nonce-1");

        assertEquals("sub-1", validated.getSubject());
    }

    @Test
    void shouldRejectIdTokenWithWrongNonce() {
        when(jwtDecoder.decode("id-token")).thenReturn(validIdToken());

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> validator.validateIdToken("id-token", "other-nonce"));
        assertEquals("TOKEN_VALIDATION_FAILED", ex.getCode());
        assertEquals(401, ex.getStatus());
    }

    @Test
    void shouldRejectIdTokenWithWrongIssuer() {
        when(jwtDecoder.decode("id-token")).thenReturn(
                idToken("nonce-1", "https://evil.example/realms/X", List.of(CLIENT_ID), CLIENT_ID,
                        Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600)));

        assertThrows(OidcGatewayException.class, () -> validator.validateIdToken("id-token", "nonce-1"));
    }

    @Test
    void shouldRejectIdTokenWithWrongAudience() {
        when(jwtDecoder.decode("id-token")).thenReturn(
                idToken("nonce-1", ISSUER, List.of("other-client"), CLIENT_ID,
                        Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600)));

        assertThrows(OidcGatewayException.class, () -> validator.validateIdToken("id-token", "nonce-1"));
    }

    @Test
    void shouldRejectIdTokenWithWrongAzp() {
        when(jwtDecoder.decode("id-token")).thenReturn(
                idToken("nonce-1", ISSUER, List.of(CLIENT_ID), "another-app",
                        Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600)));

        assertThrows(OidcGatewayException.class, () -> validator.validateIdToken("id-token", "nonce-1"));
    }

    @Test
    void shouldRejectExpiredToken() {
        when(jwtDecoder.decode("id-token")).thenReturn(
                idToken("nonce-1", ISSUER, List.of(CLIENT_ID), CLIENT_ID,
                        Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600)));

        assertThrows(OidcGatewayException.class, () -> validator.validateIdToken("id-token", "nonce-1"));
    }

    @Test
    void shouldRejectTokenWithInvalidSignatureOrExpiryHandledByDecoder() {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtException("bad signature"));

        OidcGatewayException ex = assertThrows(OidcGatewayException.class,
                () -> validator.validateIdToken("id-token", "nonce-1"));
        assertEquals("TOKEN_VALIDATION_FAILED", ex.getCode());
    }

    @Test
    void shouldAcceptValidAccessToken() {
        Jwt access = Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .issuer(ISSUER)
                .subject("sub-1")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("aud", List.of(CLIENT_ID))
                .build();
        when(jwtDecoder.decode("access-token")).thenReturn(access);

        validator.validateAccessToken("access-token");
    }

    @Test
    void shouldRejectAccessTokenWithWrongAudience() {
        Jwt access = Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .issuer(ISSUER)
                .subject("sub-1")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("aud", List.of("some-other-service"))
                .build();
        when(jwtDecoder.decode("access-token")).thenReturn(access);

        assertThrows(OidcGatewayException.class, () -> validator.validateAccessToken("access-token"));
    }

    @Test
    void shouldRejectBlankOrNullToken() {
        assertThrows(OidcGatewayException.class, () -> validator.validateIdToken("", "nonce-1"));
        assertThrows(OidcGatewayException.class, () -> validator.validateAccessToken(null));
    }
}
