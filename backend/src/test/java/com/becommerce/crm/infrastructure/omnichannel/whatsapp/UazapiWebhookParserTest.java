package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppWebhookParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UazapiWebhookParserTest {

    private UazapiWebhookParser parser;

    @BeforeEach
    void setUp() {
        parser = new UazapiWebhookParser();
    }

    @Test
    void shouldDetectUazapiFormat() {
        Map<String, Object> payload = Map.of(
                "EventType", "messages",
                "message", Map.of("chatid", "5511999999999@s.whatsapp.net"),
                "owner", "553491546422"
        );
        assertTrue(UazapiWebhookParser.isUazapiFormat(payload));
    }

    @Test
    void shouldNotDetectMetaFormatAsUazapi() {
        Map<String, Object> payload = Map.of(
                "entry", java.util.List.of()
        );
        assertFalse(UazapiWebhookParser.isUazapiFormat(payload));
    }

    @Test
    void shouldIdentifyInboundMessage() {
        Map<String, Object> payload = Map.of(
                "EventType", "messages",
                "message", Map.of(
                        "chatid", "5511999999999@s.whatsapp.net",
                        "messageid", "3EB0C7B4E7A2B8E6D4F1",
                        "text", "Ola, tudo bem?",
                        "messageType", "conversation",
                        "fromMe", false,
                        "wasSentByApi", false,
                        "senderName", "Cliente Teste"
                ),
                "owner", "553491546422"
        );

        assertTrue(parser.isInboundMessage(payload));
    }

    @Test
    void shouldNotIdentifyOutboundAsInbound() {
        Map<String, Object> payload = Map.of(
                "EventType", "messages",
                "message", Map.of(
                        "chatid", "5511999999999@s.whatsapp.net",
                        "messageid", "3EB0C7B4E7A2B8E6D4F1",
                        "text", "Resposta automatica",
                        "fromMe", true,
                        "wasSentByApi", true
                ),
                "owner", "553491546422"
        );

        assertFalse(parser.isInboundMessage(payload));
    }

    @Test
    void shouldParseInboundMessage() {
        Map<String, Object> payload = Map.of(
                "EventType", "messages",
                "message", Map.of(
                        "chatid", "5511999999999@s.whatsapp.net",
                        "messageid", "3EB0C7B4E7A2B8E6D4F1",
                        "text", "Ola, tudo bem?",
                        "messageType", "conversation",
                        "fromMe", false,
                        "wasSentByApi", false,
                        "sender", "5511999999999@s.whatsapp.net"
                ),
                "owner", "553491546422"
        );

        Optional<WhatsAppWebhookParser.InboundMessageData> result = parser.parseInboundMessage(payload);

        assertTrue(result.isPresent());
        WhatsAppWebhookParser.InboundMessageData data = result.get();
        assertEquals("3EB0C7B4E7A2B8E6D4F1", data.externalMessageId());
        assertEquals("5511999999999", data.from());
        assertEquals("553491546422", data.to());
        assertEquals("Ola, tudo bem?", data.body());
    }

    @Test
    void shouldExtractSenderFromFallbackChain() {
        // sender ausente, usa chatid
        Map<String, Object> payload = Map.of(
                "EventType", "messages",
                "message", Map.of(
                        "chatid", "5511999999999@s.whatsapp.net",
                        "messageid", "3EB0C7B4E7A2B8E6D4F1",
                        "text", "Teste",
                        "fromMe", false,
                        "wasSentByApi", false
                ),
                "owner", "553491546422"
        );

        Optional<WhatsAppWebhookParser.InboundMessageData> result = parser.parseInboundMessage(payload);

        assertTrue(result.isPresent());
        assertEquals("5511999999999", result.get().from());
    }

    @Test
    void shouldReturnChannelReference() {
        Map<String, Object> payload = Map.of(
                "EventType", "messages",
                "message", Map.of("chatid", "5511999999999@s.whatsapp.net"),
                "owner", "553491546422"
        );

        assertEquals("553491546422", parser.providerChannelReference(payload));
    }

    @Test
    void shouldReturnProviderName() {
        assertEquals("UAZAPI", parser.providerName());
    }
}
