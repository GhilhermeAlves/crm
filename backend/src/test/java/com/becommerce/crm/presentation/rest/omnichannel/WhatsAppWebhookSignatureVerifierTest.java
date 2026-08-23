package com.becommerce.crm.presentation.rest.omnichannel;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppWebhookSignatureVerifierTest {

    private static final String SECRET = "app-secret-123";
    private static final String PAYLOAD = "{\"object\":\"whatsapp_business_account\"}";

    private static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] d = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : d) {
                hex.append(String.format("%02x", b));
            }
            return "sha256=" + hex;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void validSignature_shouldBeAccepted() {
        WhatsAppWebhookSignatureVerifier verifier =
                new WhatsAppWebhookSignatureVerifier(SECRET, false);
        assertTrue(verifier.isValid(sign(SECRET, PAYLOAD), PAYLOAD));
    }

    @Test
    void invalidSignature_shouldBeRejected() {
        WhatsAppWebhookSignatureVerifier verifier =
                new WhatsAppWebhookSignatureVerifier(SECRET, false);
        assertFalse(verifier.isValid(sign("wrong-secret", PAYLOAD), PAYLOAD));
    }

    @Test
    void missingSignature_shouldBeRejectedWhenEnforced() {
        WhatsAppWebhookSignatureVerifier verifier =
                new WhatsAppWebhookSignatureVerifier(SECRET, false);
        assertFalse(verifier.isValid(null, PAYLOAD));
        assertFalse(verifier.isValid("", PAYLOAD));
    }

    @Test
    void tamperedPayload_shouldBeRejected() {
        WhatsAppWebhookSignatureVerifier verifier =
                new WhatsAppWebhookSignatureVerifier(SECRET, false);
        String signature = sign(SECRET, PAYLOAD);
        String tampered = PAYLOAD.replace("whatsapp", "whatsvil");
        assertFalse(verifier.isValid(signature, tampered));
    }

    @Test
    void unsignedRequests_shouldBeRejectedByDefault() {
        WhatsAppWebhookSignatureVerifier verifier =
                new WhatsAppWebhookSignatureVerifier("", false);
        assertFalse(verifier.isValid(null, PAYLOAD));
    }

    @Test
    void unsignedRequests_shouldBeAllowedOnlyInExplicitDevMode() {
        WhatsAppWebhookSignatureVerifier strict =
                new WhatsAppWebhookSignatureVerifier("", false);
        assertFalse(strict.isValid(null, PAYLOAD));

        WhatsAppWebhookSignatureVerifier dev =
                new WhatsAppWebhookSignatureVerifier("", true);
        assertTrue(dev.isValid(null, PAYLOAD));
    }
}
