package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvolutionWebhookParserTest {

    private final EvolutionWebhookParser parser = new EvolutionWebhookParser();

    private static Map<String, Object> json(String s) throws Exception {
        return new ObjectMapper().readValue(s, new TypeReference<>() {
        });
    }

    private static String upsert(String key, String message, String messageType) {
        return """
                {"event":"messages.upsert","instance":"comercial","data":{"key":%s,"pushName":"Joao",
                 "message":%s,"messageType":"%s","messageTimestamp":1759800000}}
                """.formatted(key, message, messageType);
    }

    private static String upsert(String key, String message) {
        return upsert(key, message, "conversation");
    }

    @Test
    void upsert_textoSimples() throws Exception {
        Map<String, Object> raw = json(upsert(
                "{\"remoteJid\":\"5511999998888@s.whatsapp.net\",\"fromMe\":false,\"id\":\"3EB0A\"}",
                "{\"conversation\":\"Oi, tudo bem?\"}"));

        assertTrue(parser.isInboundMessage(raw));
        assertFalse(parser.isStatusUpdate(raw));
        assertEquals("comercial", parser.providerChannelReference(raw));
        WhatsAppWebhookParser.InboundMessageData d = parser.parseInboundMessage(raw).orElseThrow();
        assertEquals("3EB0A", d.externalMessageId());
        assertEquals("5511999998888", d.from());
        assertEquals("comercial", d.to());
        assertEquals("Oi, tudo bem?", d.body());
        assertEquals("Joao", d.senderName());
    }

    @Test
    void upsert_eventoEmMaiusculoComUnderscore_eTextoEstendido() throws Exception {
        Map<String, Object> raw = json(upsert(
                "{\"remoteJid\":\"5511999998888@s.whatsapp.net\",\"fromMe\":false,\"id\":\"3EB0B\"}",
                "{\"extendedTextMessage\":{\"text\":\"link https://x.com\"}}")
                .replace("messages.upsert", "MESSAGES_UPSERT"));

        assertEquals("link https://x.com", parser.parseInboundMessage(raw).orElseThrow().body());
    }

    @Test
    void upsert_contatoLid_usaNumeroAlternativo() throws Exception {
        Map<String, Object> raw = json(upsert(
                "{\"remoteJid\":\"12345678901234@lid\",\"remoteJidAlt\":\"5511988887777@s.whatsapp.net\",\"fromMe\":false,\"id\":\"L1\"}",
                "{\"conversation\":\"oi\"}"));

        assertEquals("5511988887777", parser.parseInboundMessage(raw).orElseThrow().from());
    }

    @Test
    void upsert_midiaSemLegenda_usaPlaceholderDoTipo() throws Exception {
        Map<String, Object> raw = json(upsert(
                "{\"remoteJid\":\"5511999998888@s.whatsapp.net\",\"fromMe\":false,\"id\":\"A1\"}",
                "{\"audioMessage\":{\"seconds\":3}}", "audioMessage"));

        assertEquals("[audioMessage]", parser.parseInboundMessage(raw).orElseThrow().body());
    }

    @Test
    void upsert_ignoraFromMeGrupoEBroadcast() throws Exception {
        assertTrue(parser.parseInboundMessage(json(upsert(
                "{\"remoteJid\":\"5511999998888@s.whatsapp.net\",\"fromMe\":true,\"id\":\"M\"}",
                "{\"conversation\":\"eu\"}"))).isEmpty());
        assertTrue(parser.parseInboundMessage(json(upsert(
                "{\"remoteJid\":\"120363000000@g.us\",\"fromMe\":false,\"id\":\"G\"}",
                "{\"conversation\":\"grupo\"}"))).isEmpty());
        assertTrue(parser.parseInboundMessage(json(upsert(
                "{\"remoteJid\":\"status@broadcast\",\"fromMe\":false,\"id\":\"S\"}",
                "{\"conversation\":\"story\"}"))).isEmpty());
    }

    @Test
    void update_mapeiaStatus() throws Exception {
        String tpl = "{\"event\":\"messages.update\",\"instance\":\"comercial\",\"data\":{\"keyId\":\"3EB0A\","
                + "\"remoteJid\":\"5511999998888@s.whatsapp.net\",\"fromMe\":true,\"status\":\"%s\"}}";

        assertTrue(parser.isStatusUpdate(json(tpl.formatted("READ"))));
        assertEquals(MessageStatus.SENT, parser.parseStatusUpdate(json(tpl.formatted("SERVER_ACK"))).orElseThrow().status());
        assertEquals(MessageStatus.DELIVERED, parser.parseStatusUpdate(json(tpl.formatted("DELIVERY_ACK"))).orElseThrow().status());
        assertEquals(MessageStatus.READ, parser.parseStatusUpdate(json(tpl.formatted("PLAYED"))).orElseThrow().status());
        WhatsAppWebhookParser.StatusData failed = parser.parseStatusUpdate(json(tpl.formatted("ERROR"))).orElseThrow();
        assertEquals(MessageStatus.FAILED, failed.status());
        assertEquals("3EB0A", failed.externalMessageId());
        assertTrue(parser.parseStatusUpdate(json(tpl.formatted("PENDING"))).isEmpty());
    }

    @Test
    void outrosEventos_naoSaoProcessaveis() throws Exception {
        Map<String, Object> raw = json("{\"event\":\"connection.update\",\"instance\":\"comercial\",\"data\":{\"state\":\"open\"}}");
        assertFalse(parser.isInboundMessage(raw));
        assertFalse(parser.isStatusUpdate(raw));
    }
}
