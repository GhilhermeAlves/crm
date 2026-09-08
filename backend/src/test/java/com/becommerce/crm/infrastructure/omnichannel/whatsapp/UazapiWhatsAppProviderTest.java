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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.http.HttpStatus.*;

class UazapiWhatsAppProviderTest {

    private static final String BASE_URL = "https://free.uazapi.com";
    private static final String INSTANCE_TOKEN = "test-token-123";

    private RestClient.Builder builder;
    private MockRestServiceServer mockServer;
    private UazapiWhatsAppProvider provider;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        Map<String, String> env = Map.of("UAZAPI_TOKEN", INSTANCE_TOKEN);
        provider = new UazapiWhatsAppProvider(BASE_URL, builder, env::get);
    }

    private WhatsAppProvider.SendRequest request(String secretsRef) {
        return new WhatsAppProvider.SendRequest(UUID.randomUUID(), UUID.randomUUID(),
                "instance-a", "5511999998888", "Olá, tudo bem?", secretsRef);
    }

    @Test
    void send_success_withMessageId_shouldReturnId() {
        mockServer.expect(requestTo(BASE_URL + "/send/text"))
                .andExpect(header("token", INSTANCE_TOKEN))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":true,"message":"enviado","data":{"key":{"id":"true_5511999998888_abc123"}}}
                                """));

        WhatsAppProvider.SendResult result = provider.send(request(null));

        assertEquals("true_5511999998888_abc123", result.externalMessageId());
        assertEquals("UAZAPI", provider.providerName());
    }

    @Test
    void send_success_withDataId_shouldReturnId() {
        mockServer.expect(requestTo(BASE_URL + "/send/text"))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":true,"data":{"id":"msg-456"}}
                                """));

        WhatsAppProvider.SendResult result = provider.send(request(null));
        assertEquals("msg-456", result.externalMessageId());
    }

    @Test
    void send_success_withSuccessOnly_shouldReturnGeneratedId() {
        mockServer.expect(requestTo(BASE_URL + "/send/text"))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":true,"message":"enviado","data":{}}
                                """));

        WhatsAppProvider.SendResult result = provider.send(request(null));
        assertNotNull(result.externalMessageId());
        assertTrue(result.externalMessageId().startsWith("UAZAPI_"));
    }

    @Test
    void send_httpError4xx_shouldThrow() {
        mockServer.expect(requestTo(BASE_URL + "/send/text"))
                .andRespond(withStatus(BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":false,"message":"invalid phone"}
                                """));

        OmnichannelProviderException ex =
                assertThrows(OmnichannelProviderException.class, () -> provider.send(request(null)));
        assertEquals("UAZAPI HTTP 400", ex.getMessage());
    }

    @Test
    void send_httpError5xx_shouldThrow() {
        mockServer.expect(requestTo(BASE_URL + "/send/text"))
                .andRespond(withStatus(INTERNAL_SERVER_ERROR));

        assertThrows(OmnichannelProviderException.class, () -> provider.send(request(null)));
    }

    @Test
    void send_tokenFromSecretsRef_shouldBeUsedOverGlobal() {
        Map<String, String> env = Map.of(
                "UAZAPI_TOKEN", INSTANCE_TOKEN,
                "CRM_UAZAPI_CHANNEL_B_TOKEN", "channel-b-token");
        provider = new UazapiWhatsAppProvider(BASE_URL, builder, env::get);

        mockServer.expect(requestTo(BASE_URL + "/send/text"))
                .andExpect(header("token", "channel-b-token"))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":true,"data":{"key":{"id":"ch_b_msg"}}}
                                """));

        WhatsAppProvider.SendResult result =
                provider.send(request("CRM_UAZAPI_CHANNEL_B_TOKEN"));
        assertEquals("ch_b_msg", result.externalMessageId());
    }

    @Test
    void send_secretsRefSetButEnvMissing_shouldFallbackToGlobal() {
        Map<String, String> env = Map.of("UAZAPI_TOKEN", INSTANCE_TOKEN);
        provider = new UazapiWhatsAppProvider(BASE_URL, builder, env::get);

        mockServer.expect(requestTo(BASE_URL + "/send/text"))
                .andExpect(header("token", INSTANCE_TOKEN))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":true,"data":{"key":{"id":"fallback_msg"}}}
                                """));

        WhatsAppProvider.SendResult result =
                provider.send(request("CRM_UAZAPI_UNKNOWN_TOKEN"));
        assertEquals("fallback_msg", result.externalMessageId());
    }

    @Test
    void send_whenGlobalTokenMissing_shouldThrowWithoutHttpCall() {
        Map<String, String> emptyEnv = Map.of();
        UazapiWhatsAppProvider noTokenProvider =
                new UazapiWhatsAppProvider(BASE_URL, builder, emptyEnv::get);

        OmnichannelProviderException ex =
                assertThrows(OmnichannelProviderException.class, () -> noTokenProvider.send(request(null)));
        assertEquals("Credencial UAZAPI não configurada (UAZAPI_TOKEN)", ex.getMessage());
    }

    @Test
    void send_whenBaseUrlBlank_shouldThrow() {
        UazapiWhatsAppProvider noUrlProvider =
                new UazapiWhatsAppProvider("  ", builder, Map.of("UAZAPI_TOKEN", "x")::get);

        OmnichannelProviderException ex =
                assertThrows(OmnichannelProviderException.class, () -> noUrlProvider.send(request(null)));
        assertEquals("UAZAPI_BASE_URL não configurada", ex.getMessage());
    }

    @Test
    void send_responseSuccessFalse_shouldReturnNull_forId() {
        mockServer.expect(requestTo(BASE_URL + "/send/text"))
                .andRespond(withStatus(OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":false,"message":"not sent"}
                                """));

        // success=false → extractExternalId retorna null → lança exception
        assertThrows(OmnichannelProviderException.class, () -> provider.send(request(null)));
    }
}
