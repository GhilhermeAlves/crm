package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MetaCloudWhatsAppProviderTest {

    private static final String BASE_URL = "https://graph.test";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private MetaCloudWhatsAppProvider provider(Map<String, String> env) {
        return new MetaCloudWhatsAppProvider(BASE_URL + "/", "v25.0", builder, env::get);
    }

    private WhatsAppProvider.SendRequest request(String phoneNumberId, String secretsRef) {
        return new WhatsAppProvider.SendRequest(UUID.randomUUID(), UUID.randomUUID(),
                phoneNumberId, "5511999998888", "Olá!", secretsRef);
    }

    @Test
    void send_devePostarNoPhoneNumberIdComBearerERetornarWamid() {
        server.expect(requestTo(BASE_URL + "/v25.0/123456/messages"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer token-global"))
                .andExpect(content().json("""
                        {"messaging_product":"whatsapp","recipient_type":"individual","to":"5511999998888",
                         "type":"text","text":{"preview_url":false,"body":"Olá!"}}
                        """))
                .andRespond(withSuccess("{\"messaging_product\":\"whatsapp\",\"contacts\":[{\"wa_id\":\"5511999998888\"}],"
                        + "\"messages\":[{\"id\":\"wamid.OK\"}]}", MediaType.APPLICATION_JSON));

        WhatsAppProvider.SendResult result = provider(Map.of("META_WHATSAPP_ACCESS_TOKEN", "token-global"))
                .send(request("123456", null));

        assertEquals("wamid.OK", result.externalMessageId());
        server.verify();
    }

    @Test
    void send_devePreferirTokenDoSecretsRefDoCanal() {
        server.expect(requestTo(BASE_URL + "/v25.0/123456/messages"))
                .andExpect(header("Authorization", "Bearer token-canal"))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.C\"}]}", MediaType.APPLICATION_JSON));

        provider(Map.of("META_WHATSAPP_ACCESS_TOKEN", "token-global", "META_TOKEN_CLINICA", "token-canal"))
                .send(request("123456", "META_TOKEN_CLINICA"));
        server.verify();
    }

    @Test
    void send_erroHttp_lancaProviderException() {
        server.expect(requestTo(BASE_URL + "/v25.0/123456/messages")).andRespond(withStatus(BAD_REQUEST));

        OmnichannelProviderException e = assertThrows(OmnichannelProviderException.class,
                () -> provider(Map.of("META_WHATSAPP_ACCESS_TOKEN", "t")).send(request("123456", null)));
        assertEquals("Meta HTTP 400", e.getMessage());
    }

    @Test
    void send_semToken_lancaProviderException() {
        assertThrows(OmnichannelProviderException.class, () -> provider(Map.of()).send(request("123456", null)));
    }

    @Test
    void send_semPhoneNumberId_lancaProviderException() {
        assertThrows(OmnichannelProviderException.class,
                () -> provider(Map.of("META_WHATSAPP_ACCESS_TOKEN", "t")).send(request(" ", null)));
    }
}
