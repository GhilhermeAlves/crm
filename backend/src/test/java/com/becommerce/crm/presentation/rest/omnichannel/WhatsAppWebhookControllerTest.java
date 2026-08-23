package com.becommerce.crm.presentation.rest.omnichannel;

import com.becommerce.crm.application.omnichannel.port.input.WhatsAppWebhookUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhatsAppWebhookControllerTest {

    private static final String SECRET = "app-secret-123";
    private static final String PAYLOAD = "{\"object\":\"whatsapp_business_account\"}";

    private final WhatsAppWebhookUseCase useCase = mock(WhatsAppWebhookUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(
                        useCase, new WhatsAppWebhookSignatureVerifier(SECRET, false), new ObjectMapper()))
                .build();
    }

    private String signature(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
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
    void validSignature_shouldProcessAndReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/omnichannel/whatsapp/webhook")
                        .header("X-Hub-Signature-256", signature(PAYLOAD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(useCase).handleEvent(captor.capture());
        assertEquals("whatsapp_business_account", captor.getValue().get("object"));
    }

    @Test
    void invalidSignature_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/omnichannel/whatsapp/webhook")
                        .header("X-Hub-Signature-256", signature("wrong-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(any());
    }

    @Test
    void missingSignature_shouldReturn401WhenEnforced() throws Exception {
        mockMvc.perform(post("/api/v1/omnichannel/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(any());
    }

    @Test
    void tamperedPayload_shouldReturn401() throws Exception {
        String tampered = PAYLOAD.replace("whatsapp", "whatsvil");
        mockMvc.perform(post("/api/v1/omnichannel/whatsapp/webhook")
                        .header("X-Hub-Signature-256", signature(PAYLOAD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tampered))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(any());
    }
}
