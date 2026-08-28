package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Function;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

class WhatsAppCloudApiProviderTest {

    private static final String GRAPH_URL = "https://graph.facebook.com/v19.0";
    private static final String PHONE_ID = "1234567890";
    private static final String GLOBAL_TOKEN = "global-access-token";

    private RestClient.Builder builder;
    private MockRestServiceServer mockServer;
    private WhatsAppCloudApiProvider provider;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        Map<String, String> env = Map.of("CRM_WHATSAPP_ACCESS_TOKEN", GLOBAL_TOKEN);
        provider = new WhatsAppCloudApiProvider(GRAPH_URL, builder, env::get);
    }

    private WhatsAppProvider.SendRequest request(String secretsRef) {
        return new WhatsAppProvider.SendRequest(UUID.randomUUID(), UUID.randomUUID(),
                PHONE_ID, "5511999998888", "Olá, tudo bem?", secretsRef);
    }

    @Test
    void send_success_shouldReturnWamid() {
        mockServer.expect(requestTo(GRAPH_URL + "/" + PHONE_ID + "/messages"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + GLOBAL_TOKEN))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.ABC123\"}]}"));

        WhatsAppProvider.SendResult result = provider.send(request(null));

        assertEquals("wamid.ABC123", result.externalMessageId());
        assertEquals("WHATSAPP_CLOUD_API", provider.providerName());
    }

    @Test
    void send_responseWithoutWamid_shouldThrow() {
        mockServer.expect(requestTo(GRAPH_URL + "/" + PHONE_ID + "/messages"))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messaging_product\":\"whatsapp\",\"messages\":[]}"));

        assertThrows(OmnichannelProviderException.class, () -> provider.send(request(null)));
    }

    @Test
    void send_httpError4xx_shouldThrow() {
        mockServer.expect(requestTo(GRAPH_URL + "/" + PHONE_ID + "/messages"))
                .andRespond(withStatus(BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"bad token\"}}"));

        OmnichannelProviderException ex =
                assertThrows(OmnichannelProviderException.class, () -> provider.send(request(null)));
        assertEquals("WhatsApp Cloud API HTTP 400", ex.getMessage());
    }

    @Test
    void send_httpError5xx_shouldThrow() {
        mockServer.expect(requestTo(GRAPH_URL + "/" + PHONE_ID + "/messages"))
                .andRespond(withStatus(INTERNAL_SERVER_ERROR));

        assertThrows(OmnichannelProviderException.class, () -> provider.send(request(null)));
    }

    @Test
    void send_whenGlobalTokenMissing_shouldThrowWithoutHttpCall() {
        Map<String, String> emptyEnv = Map.of();
        WhatsAppCloudApiProvider noTokenProvider =
                new WhatsAppCloudApiProvider(GRAPH_URL, builder, emptyEnv::get);

        OmnichannelProviderException ex =
                assertThrows(OmnichannelProviderException.class, () -> noTokenProvider.send(request(null)));
        assertEquals(
                "Credencial de WhatsApp não configurada (CRM_WHATSAPP_ACCESS_TOKEN)", ex.getMessage());
    }

    @Test
    void send_channelTokenFromSecretsRef_shouldBeUsedOverGlobal() {
        Map<String, String> env = Map.of(
                "CRM_WHATSAPP_ACCESS_TOKEN", GLOBAL_TOKEN,
                "CRM_WHATSAPP_ACCOUNT_BANCOS_TOKEN", "channel-b-token");
        provider = new WhatsAppCloudApiProvider(GRAPH_URL, builder, env::get);

        mockServer.expect(requestTo(GRAPH_URL + "/" + PHONE_ID + "/messages"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer channel-b-token"))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messages\":[{\"id\":\"wamid.CHANNELB\"}]}"));

        WhatsAppProvider.SendResult result =
                provider.send(request("CRM_WHATSAPP_ACCOUNT_BANCOS_TOKEN"));
        assertEquals("wamid.CHANNELB", result.externalMessageId());
    }

    @Test
    void send_secretsRefSetButEnvMissing_shouldFallbackToGlobal() {
        Map<String, String> env = Map.of("CRM_WHATSAPP_ACCESS_TOKEN", GLOBAL_TOKEN);
        provider = new WhatsAppCloudApiProvider(GRAPH_URL, builder, env::get);

        mockServer.expect(requestTo(GRAPH_URL + "/" + PHONE_ID + "/messages"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + GLOBAL_TOKEN))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messages\":[{\"id\":\"wamid.FALLBACK\"}]}"));

        WhatsAppProvider.SendResult result = provider.send(request("CRM_WHATSAPP_ACCOUNT_UNKNOWN_TOKEN"));
        assertEquals("wamid.FALLBACK", result.externalMessageId());
    }
}