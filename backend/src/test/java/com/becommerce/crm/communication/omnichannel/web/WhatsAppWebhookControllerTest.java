package com.becommerce.crm.communication.omnichannel.web;

import com.becommerce.crm.communication.omnichannel.application.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.WhatsAppWebhookTokenVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhatsAppWebhookControllerTest {

    private static final String URL = "/api/v1/omnichannel/whatsapp/webhook";
    private static final String TOKEN = "segredo-token";
    private static final String PAYLOAD = "{\"event\":\"messages.upsert\",\"instance\":\"comercial\"}";

    private final WhatsAppWebhookUseCase useCase = mock(WhatsAppWebhookUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(
                        useCase, new WhatsAppWebhookTokenVerifier(false, TOKEN), new ObjectMapper()))
                .build();
    }

    @Test
    void tokenNaQuery_deveProcessar() throws Exception {
        mockMvc.perform(post(URL).param("token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(useCase).handleEvent(captor.capture());
        assertEquals("comercial", captor.getValue().get("instance"));
    }

    @Test
    void tokenNoHeader_deveProcessar() throws Exception {
        mockMvc.perform(post(URL).header("X-Webhook-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isOk());
        verify(useCase).handleEvent(any());
    }

    @Test
    void semToken_deveRetornar401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(any());
    }

    @Test
    void tokenErrado_deveRetornar401() throws Exception {
        mockMvc.perform(post(URL).param("token", "outro")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(any());
    }

    @Test
    void payloadInvalido_deveRetornar400() throws Exception {
        mockMvc.perform(post(URL).param("token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("nao-e-json"))
                .andExpect(status().isBadRequest());
        verify(useCase, never()).handleEvent(any());
    }
}
