package com.becommerce.crm.communication.omnichannel.infrastructure.instagram;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaInstagramWebhookParserTest {

    private final MetaInstagramWebhookParser parser = new MetaInstagramWebhookParser();

    private static Map<String, Object> json(String s) throws Exception {
        return new ObjectMapper().readValue(s, new TypeReference<>() {
        });
    }

    @Test
    void mensagemDeTextoEImagem_viramEventosSeparados() throws Exception {
        Map<String, Object> raw = json("""
                {"object":"instagram","entry":[{"id":"1784","time":1,"messaging":[
                  {"sender":{"id":"999"},"recipient":{"id":"1784"},"timestamp":1,
                   "message":{"mid":"mid.A","text":"Quanto custa o kit?"}},
                  {"sender":{"id":"999"},"recipient":{"id":"1784"},"timestamp":2,
                   "message":{"mid":"mid.B","attachments":[{"type":"image","payload":{"url":"https://x"}}]}},
                  {"sender":{"id":"999"},"recipient":{"id":"1784"},"timestamp":3,"read":{"mid":"mid.A"}}
                ]}]}
                """);

        List<Map<String, Object>> events = parser.splitEvents(raw);

        assertEquals(2, events.size());
        WhatsAppWebhookParser.InboundMessageData text = parser.parseInboundMessage(events.get(0)).orElseThrow();
        assertEquals("mid.A", text.externalMessageId());
        assertEquals("999", text.from());
        assertEquals("1784", text.to());
        assertEquals("Quanto custa o kit?", text.body());
        assertFalse(text.fromMe());
        assertEquals(MessageType.TEXT, text.type());
        assertEquals("1784", parser.providerChannelReference(events.get(0)));
        assertTrue(parser.isInboundMessage(events.get(0)));
        assertFalse(parser.isStatusUpdate(events.get(0)));

        WhatsAppWebhookParser.InboundMessageData image = parser.parseInboundMessage(events.get(1)).orElseThrow();
        assertEquals(MessageType.IMAGE, image.type());
        assertEquals("[image]", image.body());
    }

    @Test
    void eco_eMensagemDaPropriaConta_comClienteNoRecipient() throws Exception {
        Map<String, Object> raw = json("""
                {"object":"instagram","entry":[{"id":"1784","messaging":[
                  {"sender":{"id":"1784"},"recipient":{"id":"999"},
                   "message":{"mid":"mid.E","text":"Oi! Segue o catálogo","is_echo":true}}]}]}
                """);

        WhatsAppWebhookParser.InboundMessageData echo =
                parser.parseInboundMessage(parser.splitEvents(raw).get(0)).orElseThrow();

        assertTrue(echo.fromMe());
        assertEquals("999", echo.from());
        assertEquals("1784", echo.to());
    }

    @Test
    void postback_viraTextoComTituloDoBotao() throws Exception {
        Map<String, Object> raw = json("""
                {"object":"instagram","entry":[{"id":"1784","messaging":[
                  {"sender":{"id":"999"},"recipient":{"id":"1784"},
                   "postback":{"mid":"mid.P","title":"Ver preços","payload":"PRECOS"}}]}]}
                """);

        WhatsAppWebhookParser.InboundMessageData data =
                parser.parseInboundMessage(parser.splitEvents(raw).get(0)).orElseThrow();

        assertEquals("mid.P", data.externalMessageId());
        assertEquals("Ver preços", data.body());
    }

    @Test
    void mensagemApagada_eDescarteEsperado() throws Exception {
        Map<String, Object> event = parser.splitEvents(json("""
                {"object":"instagram","entry":[{"id":"1784","messaging":[
                  {"sender":{"id":"999"},"recipient":{"id":"1784"},
                   "message":{"mid":"mid.D","is_deleted":true}}]}]}
                """)).get(0);

        assertTrue(parser.parseInboundMessage(event).isEmpty());
        assertTrue(parser.isExpectedSkip(parser.inboundSkipReason(event)));
    }

    @Test
    void payloadDoWhatsApp_naoEInstagram() throws Exception {
        assertFalse(MetaInstagramWebhookParser.isInstagramPayload(json(
                "{\"object\":\"whatsapp_business_account\",\"entry\":[]}")));
        assertTrue(MetaInstagramWebhookParser.isInstagramPayload(json("{\"object\":\"instagram\",\"entry\":[]}")));
    }
}
