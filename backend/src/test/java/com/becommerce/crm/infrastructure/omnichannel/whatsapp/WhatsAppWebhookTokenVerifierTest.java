package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppWebhookTokenVerifierTest {

    private static final String TOKEN = "segredo-webhook-abc";

    private static WhatsAppWebhookTokenVerifier newVerifier(boolean allowUnsigned, String token) {
        return new WhatsAppWebhookTokenVerifier(allowUnsigned, token);
    }

    @Test
    void semTokenConfigurado_eAllowUnsignedFalse_deveRejeitar() {
        WhatsAppWebhookTokenVerifier v = newVerifier(false, "");
        assertFalse(v.isAuthenticated("{}", null, null));
    }

    @Test
    void tokenConfigurado_aceitaQueryCorreta() {
        WhatsAppWebhookTokenVerifier v = newVerifier(false, TOKEN);
        assertTrue(v.isAuthenticated("{}", null, TOKEN));
    }

    @Test
    void tokenConfigurado_rejeitaAusenteOuErrado() {
        WhatsAppWebhookTokenVerifier v = newVerifier(false, TOKEN);
        assertFalse(v.isAuthenticated("{}", null, null));
        assertFalse(v.isAuthenticated("{}", null, "errado"));
    }

    @Test
    void modoDesenvolvimento_allowUnsignedTrue_aceita() {
        WhatsAppWebhookTokenVerifier v = newVerifier(true, "");
        assertTrue(v.isAuthenticated("{}", null, null));
    }
}