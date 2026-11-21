package com.becommerce.crm.communication.omnichannel.web;

import com.becommerce.crm.communication.omnichannel.application.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.WhatsAppWebhookTokenVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstagramWebhookControllerTest {

    private static final String URL = InstagramWebhookController.PATH;
    private static final String SECRET = "segredo-ig";
    private static final String BODY = "{\"object\":\"instagram\",\"entry\":[{\"id\":\"1784\",\"messaging\":[]}]}";

    private final WhatsAppWebhookUseCase useCase = mock(WhatsAppWebhookUseCase.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new InstagramWebhookController(useCase,
            new WhatsAppWebhookTokenVerifier(false, "", "verifica-ig", SECRET), new ObjectMapper())).build();

    private static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void handshakeComVerifyTokenCorreto_devolveChallenge() throws Exception {
        mvc.perform(get(URL).param("hub.mode", "subscribe").param("hub.verify_token", "verifica-ig")
                        .param("hub.challenge", "42"))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void handshakeComVerifyTokenErrado_eRecusado() throws Exception {
        mvc.perform(get(URL).param("hub.mode", "subscribe").param("hub.verify_token", "outro")
                        .param("hub.challenge", "42"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postAssinado_eProcessadoSemPrecisarDeToken() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header("X-Hub-Signature-256", sign(BODY, SECRET)))
                .andExpect(status().isOk());

        verify(useCase).handleEvent(anyMap());
    }

    @Test
    void postComAssinaturaInvalida_eRecusado() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header("X-Hub-Signature-256", sign(BODY, "errado")))
                .andExpect(status().isUnauthorized());

        verify(useCase, never()).handleEvent(anyMap());
    }

    @Test
    void postSemAssinatura_eRecusado() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verify(useCase, never()).handleEvent(anyMap());
    }
}
