package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaWhatsAppWebhookParserTest {

    private final MetaWhatsAppWebhookParser parser = new MetaWhatsAppWebhookParser();

    private static Map<String, Object> json(String s) throws Exception {
        return new ObjectMapper().readValue(s, new TypeReference<>() {
        });
    }

    private static final String MESSAGES = """
            {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"messages","value":{
              "messaging_product":"whatsapp",
              "metadata":{"display_phone_number":"551130000000","phone_number_id":"123456"},
              "contacts":[{"profile":{"name":"Maria"},"wa_id":"5511999998888"}],
              "messages":[
                {"from":"5511999998888","id":"wamid.A","timestamp":"1","type":"text","text":{"body":"Oi"}},
                {"from":"5511999998888","id":"wamid.B","timestamp":"2","type":"audio","audio":{"id":"MEDIA1"}}
              ]}}]}]}
            """;

    @Test
    void splitEvents_deveGerarUmEventoPorMensagem() throws Exception {
        List<Map<String, Object>> events = parser.splitEvents(json(MESSAGES));

        assertEquals(2, events.size());
        WhatsAppWebhookParser.InboundMessageData first = parser.parseInboundMessage(events.get(0)).orElseThrow();
        assertEquals("wamid.A", first.externalMessageId());
        assertEquals("5511999998888", first.from());
        assertEquals("123456", first.to());
        assertEquals("Oi", first.body());
        assertEquals("Maria", first.senderName());
        assertFalse(first.fromMe());
        assertEquals(MessageType.TEXT, first.type());

        WhatsAppWebhookParser.InboundMessageData second = parser.parseInboundMessage(events.get(1)).orElseThrow();
        assertEquals("wamid.B", second.externalMessageId());
        assertEquals(MessageType.AUDIO, second.type());
        assertEquals("[audio]", second.body());
        assertEquals("123456", parser.providerChannelReference(events.get(1)));
        assertTrue(parser.isInboundMessage(events.get(1)));
        assertFalse(parser.isStatusUpdate(events.get(1)));
    }

    @Test
    void status_deveMapearSentDeliveredReadFailed() throws Exception {
        Map<String, Object> raw = json("""
                {"object":"whatsapp_business_account","entry":[{"changes":[{"field":"messages","value":{
                  "metadata":{"phone_number_id":"123456"},
                  "statuses":[
                    {"id":"wamid.1","status":"sent","recipient_id":"5511"},
                    {"id":"wamid.2","status":"delivered"},
                    {"id":"wamid.3","status":"read"},
                    {"id":"wamid.4","status":"failed","errors":[{"code":131047,"title":"Re-engagement message"}]}
                  ]}}]}]}
                """);
        List<Map<String, Object>> events = parser.splitEvents(raw);

        assertEquals(4, events.size());
        assertTrue(parser.isStatusUpdate(events.get(0)));
        assertFalse(parser.isInboundMessage(events.get(0)));
        assertEquals(MessageStatus.SENT, parser.parseStatusUpdate(events.get(0)).orElseThrow().status());
        assertEquals(MessageStatus.DELIVERED, parser.parseStatusUpdate(events.get(1)).orElseThrow().status());
        assertEquals(MessageStatus.READ, parser.parseStatusUpdate(events.get(2)).orElseThrow().status());
        WhatsAppWebhookParser.StatusData failed = parser.parseStatusUpdate(events.get(3)).orElseThrow();
        assertEquals("wamid.4", failed.externalMessageId());
        assertEquals(MessageStatus.FAILED, failed.status());
        assertEquals("Meta 131047 Re-engagement message", failed.error());
    }

    @Test
    void respostaDeBotaoInterativo_usaOTituloComoTexto() throws Exception {
        Map<String, Object> raw = json("""
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{
                  "metadata":{"phone_number_id":"123456"},
                  "messages":[{"from":"5511","id":"wamid.X","type":"interactive",
                    "interactive":{"type":"button_reply","button_reply":{"id":"b1","title":"Quero agendar"}}}]}}]}]}
                """);
        assertEquals("Quero agendar",
                parser.parseInboundMessage(parser.splitEvents(raw).get(0)).orElseThrow().body());
    }

    @Test
    void payloadSemItens_naoGeraEventos() throws Exception {
        assertTrue(parser.splitEvents(json("{\"object\":\"whatsapp_business_account\",\"entry\":[]}")).isEmpty());
    }

    @Test
    void isMetaPayload_distingueDaEvolution() throws Exception {
        assertTrue(MetaWhatsAppWebhookParser.isMetaPayload(json(MESSAGES)));
        assertFalse(MetaWhatsAppWebhookParser.isMetaPayload(
                json("{\"event\":\"messages.upsert\",\"instance\":\"comercial\"}")));
    }
}
