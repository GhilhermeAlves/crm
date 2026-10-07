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
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EvolutionWhatsAppProviderTest {

    private static final String BASE_URL = "http://evolution-api:8080";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private EvolutionWhatsAppProvider provider(Map<String, String> env) {
        return new EvolutionWhatsAppProvider(BASE_URL + "/", builder, env::get);
    }

    private WhatsAppProvider.SendRequest request(String instance, String secretsRef) {
        return new WhatsAppProvider.SendRequest(UUID.randomUUID(), UUID.randomUUID(),
                instance, "5511999998888", "Olá!", secretsRef);
    }

    @Test
    void send_deveChamarSendTextDaInstanciaERetornarKeyId() {
        server.expect(requestTo(BASE_URL + "/message/sendText/comercial"))
                .andExpect(method(POST))
                .andExpect(header("apikey", "global-key"))
                .andExpect(content().json("{\"number\":\"5511999998888\",\"text\":\"Olá!\"}"))
                .andRespond(withSuccess("{\"key\":{\"remoteJid\":\"5511999998888@s.whatsapp.net\",\"fromMe\":true,\"id\":\"3EB0ABC\"},\"status\":\"PENDING\"}",
                        MediaType.APPLICATION_JSON));

        WhatsAppProvider.SendResult result = provider(Map.of("EVOLUTION_API_KEY", "global-key"))
                .send(request("comercial", null));

        assertEquals("3EB0ABC", result.externalMessageId());
        assertEquals("EVOLUTION", provider(Map.of()).providerName());
        server.verify();
    }

    @Test
    void send_secretsRefDoCanal_temPrioridadeSobreChaveGlobal() {
        server.expect(requestTo(BASE_URL + "/message/sendText/comercial"))
                .andExpect(header("apikey", "canal-key"))
                .andRespond(withSuccess("{\"key\":{\"id\":\"X1\"}}", MediaType.APPLICATION_JSON));

        provider(Map.of("EVOLUTION_API_KEY", "global-key", "EVO_KEY_COMERCIAL", "canal-key"))
                .send(request("comercial", "EVO_KEY_COMERCIAL"));
        server.verify();
    }

    @Test
    void send_httpErro_lancaProviderException() {
        server.expect(requestTo(BASE_URL + "/message/sendText/comercial"))
                .andRespond(withStatus(UNAUTHORIZED));

        OmnichannelProviderException e = assertThrows(OmnichannelProviderException.class,
                () -> provider(Map.of("EVOLUTION_API_KEY", "k")).send(request("comercial", null)));
        assertEquals("Evolution HTTP 401", e.getMessage());
    }

    @Test
    void send_respostaSemId_lancaProviderException() {
        server.expect(requestTo(BASE_URL + "/message/sendText/comercial"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(OmnichannelProviderException.class,
                () -> provider(Map.of("EVOLUTION_API_KEY", "k")).send(request("comercial", null)));
    }

    @Test
    void send_semChave_semInstancia_ouSemBaseUrl_lancaProviderException() {
        assertThrows(OmnichannelProviderException.class,
                () -> provider(Map.of()).send(request("comercial", null)));
        assertThrows(OmnichannelProviderException.class,
                () -> provider(Map.of("EVOLUTION_API_KEY", "k")).send(request(" ", null)));
        assertThrows(OmnichannelProviderException.class,
                () -> new EvolutionWhatsAppProvider("", builder, Map.of("EVOLUTION_API_KEY", "k")::get)
                        .send(request("comercial", null)));
    }
}
