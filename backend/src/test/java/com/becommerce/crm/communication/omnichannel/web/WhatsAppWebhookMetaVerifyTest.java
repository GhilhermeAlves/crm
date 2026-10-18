package com.becommerce.crm.communication.omnichannel.web;

import com.becommerce.crm.communication.omnichannel.application.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.WhatsAppWebhookTokenVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhatsAppWebhookMetaVerifyTest {

    private static final String URL = "/api/v1/omnichannel/whatsapp/webhook";

    private MockMvc mvc(String metaVerifyToken) {
        return MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(
                        mock(WhatsAppWebhookUseCase.class),
                        new WhatsAppWebhookTokenVerifier(false, "t", metaVerifyToken), new ObjectMapper()))
                .build();
    }

    @Test
    void subscribeComVerifyTokenCorreto_deveDevolverChallengeEmTextoPuro() throws Exception {
        mvc("verifica-meta").perform(get(URL).param("hub.mode", "subscribe")
                        .param("hub.verify_token", "verifica-meta").param("hub.challenge", "1158201444"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string("1158201444"));
    }

    @Test
    void verifyTokenErrado_deveRetornar403() throws Exception {
        mvc("verifica-meta").perform(get(URL).param("hub.mode", "subscribe")
                        .param("hub.verify_token", "outro").param("hub.challenge", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void modoDiferenteDeSubscribe_deveRetornar403() throws Exception {
        mvc("verifica-meta").perform(get(URL).param("hub.mode", "unsubscribe")
                        .param("hub.verify_token", "verifica-meta").param("hub.challenge", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void semVerifyTokenConfigurado_deveRecusar() throws Exception {
        mvc("").perform(get(URL).param("hub.mode", "subscribe")
                        .param("hub.verify_token", "").param("hub.challenge", "1"))
                .andExpect(status().isForbidden());
    }
}
