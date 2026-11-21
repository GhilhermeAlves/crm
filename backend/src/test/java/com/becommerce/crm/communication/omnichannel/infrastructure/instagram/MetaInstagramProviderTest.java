package com.becommerce.crm.communication.omnichannel.infrastructure.instagram;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MetaInstagramProviderTest {

    private static final String BASE_URL = "https://graph.instagram.test";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private MetaInstagramProvider provider(Map<String, String> env) {
        return new MetaInstagramProvider(BASE_URL + "/", "v25.0", builder, env::get);
    }

    private static WhatsAppProvider.SendRequest request(String body, String secretsRef) {
        return new WhatsAppProvider.SendRequest(UUID.randomUUID(), UUID.randomUUID(), "1784", "999", body, secretsRef);
    }

    @Test
    void send_devePostarEmMeMessagesComRecipientIgsid() {
        server.expect(requestTo(BASE_URL + "/v25.0/me/messages"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer token-ig"))
                .andExpect(content().json("{\"recipient\":{\"id\":\"999\"},\"message\":{\"text\":\"Oi!\"}}"))
                .andRespond(withSuccess("{\"recipient_id\":\"999\",\"message_id\":\"mid.OK\"}",
                        MediaType.APPLICATION_JSON));

        WhatsAppProvider.SendResult result = provider(Map.of("META_INSTAGRAM_ACCESS_TOKEN", "token-ig"))
                .send(request("Oi!", null));

        assertEquals("mid.OK", result.externalMessageId());
        server.verify();
    }

    @Test
    void send_usaTokenDoSecretsRefDoCanal() {
        server.expect(requestTo(BASE_URL + "/v25.0/me/messages"))
                .andExpect(header("Authorization", "Bearer token-canal"))
                .andRespond(withSuccess("{\"message_id\":\"mid.C\"}", MediaType.APPLICATION_JSON));

        provider(Map.of("IG_ALVES", "token-canal", "META_INSTAGRAM_ACCESS_TOKEN", "global"))
                .send(request("Oi", "IG_ALVES"));

        server.verify();
    }

    @Test
    void textoLongo_eEnviadoEmPartesDeAte1000Bytes() {
        String longo = ("Kit de Natal com Papai Noel, árvore e casinha iluminada. ").repeat(40);
        List<String> parts = MetaInstagramProvider.split(longo);
        assertTrue(parts.size() > 1);
        parts.forEach(p -> assertTrue(p.getBytes(StandardCharsets.UTF_8).length <= 1000));
        assertEquals(longo.strip().replace(" ", ""), String.join("", parts).replace(" ", ""));

        server.expect(times(parts.size()), requestTo(BASE_URL + "/v25.0/me/messages"))
                .andRespond(withSuccess("{\"message_id\":\"mid.N\"}", MediaType.APPLICATION_JSON));
        assertEquals("mid.N", provider(Map.of("META_INSTAGRAM_ACCESS_TOKEN", "t")).send(request(longo, null))
                .externalMessageId());
        server.verify();
    }

    @Test
    void erroHttp_viraOmnichannelProviderException() {
        server.expect(requestTo(BASE_URL + "/v25.0/me/messages")).andRespond(withStatus(BAD_REQUEST));

        assertThrows(OmnichannelProviderException.class,
                () -> provider(Map.of("META_INSTAGRAM_ACCESS_TOKEN", "t")).send(request("Oi", null)));
    }

    @Test
    void semToken_falhaSemChamarAApi() {
        assertThrows(OmnichannelProviderException.class, () -> provider(Map.of()).send(request("Oi", null)));
        server.verify();
    }
}
