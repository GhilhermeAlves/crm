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

class WhatsAppWebhookMetaVerifyTest {

    private static final String URL = "/api/v1/omnichannel/whatsapp/webhook";
    private static final String APP_SECRET = "segredo-do-app";
    // Acentos escapados como a Meta envia; a assinatura é sobre estes bytes exatos.
    private static final String META_BODY =
            "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"changes\":[{\"value\":{\"text\":\"Ol\\u00e1\"}}]}]}";

    private final WhatsAppWebhookUseCase useCase = mock(WhatsAppWebhookUseCase.class);

    private MockMvc mvc(String metaVerifyToken) {
        return mvc(metaVerifyToken, APP_SECRET);
    }

    private MockMvc mvc(String metaVerifyToken, String appSecret) {
        return MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(
                        useCase,
                        new WhatsAppWebhookTokenVerifier(false, "t", metaVerifyToken, appSecret), new ObjectMapper()))
                .build();
    }

    private static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
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

    @Test
    void postDaMetaComAssinaturaValida_deveProcessar() throws Exception {
        mvc("v").perform(post(URL).param("token", "t").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", sign(META_BODY, APP_SECRET)).content(META_BODY))
                .andExpect(status().isOk());
        verify(useCase).handleEvent(anyMap());
    }

    @Test
    void postDaMetaComAssinaturaInvalida_deveRetornar401() throws Exception {
        mvc("v").perform(post(URL).param("token", "t").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", sign(META_BODY, "outro-segredo")).content(META_BODY))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(anyMap());
    }

    @Test
    void postDaMetaSemAssinatura_deveRetornar401() throws Exception {
        mvc("v").perform(post(URL).param("token", "t").contentType(MediaType.APPLICATION_JSON).content(META_BODY))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(anyMap());
    }

    @Test
    void postDaMetaSemAppSecretConfigurado_deveRetornar401() throws Exception {
        mvc("v", "").perform(post(URL).param("token", "t").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", sign(META_BODY, APP_SECRET)).content(META_BODY))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(anyMap());
    }

    @Test
    void postDaEvolution_naoExigeAssinatura() throws Exception {
        mvc("v").perform(post(URL).param("token", "t").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"messages.upsert\",\"instance\":\"x\"}"))
                .andExpect(status().isOk());
        verify(useCase).handleEvent(anyMap());
    }
}
