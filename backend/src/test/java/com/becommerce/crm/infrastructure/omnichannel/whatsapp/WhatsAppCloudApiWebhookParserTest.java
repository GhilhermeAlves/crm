package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppCloudApiWebhookParserTest {

    private WhatsAppCloudApiWebhookParser parser;

    @BeforeEach
    void setUp() {
        parser = new WhatsAppCloudApiWebhookParser();
    }

    @Test
    void providerName_shouldBeCloudApi() {
        assertEquals("WHATSAPP_CLOUD_API", parser.providerName());
    }

    @Test
    void isInboundMessage_validInbound_shouldBeTrue() {
        assertTrue(parser.isInboundMessage(inboundMessagePayload()));
    }

    @Test
    void isInboundMessage_emptyMessages_shouldBeFalse() {
        Map<String, Object> payload = Map.of("entry", List.of(
                Map.of("changes", List.of(
                        Map.of("value", Map.of("messages", List.of()))))));
        assertFalse(parser.isInboundMessage(payload));
    }

    @Test
    void isInboundMessage_statusOnly_shouldBeFalse() {
        assertFalse(parser.isInboundMessage(statusUpdatePayload("delivered")));
    }

    @Test
    void isStatusUpdate_validStatus_shouldBeTrue() {
        assertTrue(parser.isStatusUpdate(statusUpdatePayload("delivered")));
    }

    @Test
    void isStatusUpdate_emptyStatuses_shouldBeFalse() {
        Map<String, Object> payload = Map.of("entry", List.of(
                Map.of("changes", List.of(
                        Map.of("value", Map.of("statuses", List.of()))))));
        assertFalse(parser.isStatusUpdate(payload));
    }

    @Test
    void isStatusUpdate_messageOnly_shouldBeFalse() {
        assertFalse(parser.isStatusUpdate(inboundMessagePayload()));
    }

    @Test
    void parseInboundMessage_shouldExtractAllFields() {
        Optional<WhatsAppWebhookParser.InboundMessageData> result =
                parser.parseInboundMessage(inboundMessagePayload());

        assertTrue(result.isPresent());
        WhatsAppWebhookParser.InboundMessageData data = result.get();
        assertEquals("wamid.ABEGH1B2A3C4D5E6F7", data.externalMessageId());
        assertEquals("5511999998888", data.from());
        assertEquals("1234567890", data.to());
        assertEquals("Olá, tudo bem?", data.body());
    }

    @Test
    void parseInboundMessage_withoutFrom_shouldBeEmpty() {
        Map<String, Object> payload = messagePayloadMissingField("from");
        assertTrue(parser.parseInboundMessage(payload).isEmpty());
    }

    @Test
    void providerChannelReference_shouldBeDerivedFromMetadata() {
        assertEquals("1234567890", parser.providerChannelReference(inboundMessagePayload()));
        assertEquals("1234567890", parser.providerChannelReference(statusUpdatePayload("read")));
    }

    @Test
    void parseStatusUpdate_delivered_shouldMap() {
        assertStatus("delivered", MessageStatus.DELIVERED, null);
    }

    @Test
    void parseStatusUpdate_sent_shouldMap() {
        assertStatus("sent", MessageStatus.SENT, null);
    }

    @Test
    void parseStatusUpdate_read_shouldMap() {
        assertStatus("read", MessageStatus.READ, null);
    }

    @Test
    void parseStatusUpdate_failed_shouldMapAndCaptureError() {
        Map<String, Object> payload = Map.of("entry", List.of(
                Map.of("changes", List.of(
                        Map.of("value", Map.of(
                                "metadata", Map.of("phone_number_id", "1234567890"),
                                "statuses", List.of(Map.of(
                                        "id", "wamid.STATUS1",
                                        "status", "failed",
                                        "errors", List.of(Map.of("message", "Number of message templates exceeded"))))))))));

        Optional<WhatsAppWebhookParser.StatusData> result = parser.parseStatusUpdate(payload);
        assertTrue(result.isPresent());
        assertEquals("wamid.STATUS1", result.get().externalMessageId());
        assertEquals(MessageStatus.FAILED, result.get().status());
        assertEquals("Number of message templates exceeded", result.get().error());
    }

    @Test
    void parseStatusUpdate_unknownStatus_shouldMapToEmpty() {
        Map<String, Object> payload = Map.of("entry", List.of(
                Map.of("changes", List.of(
                        Map.of("value", Map.of(
                                "metadata", Map.of("phone_number_id", "1234567890"),
                                "statuses", List.of(Map.of("id", "wamid.X", "status", "benz"))))))));

        assertTrue(parser.parseStatusUpdate(payload).isEmpty());
    }

    @Test
    void parseVerification_shouldExtractHubParameters() {
        WhatsAppWebhookParser.Verification verification = parser.parseVerification(Map.of(
                "hub.mode", "subscribe",
                "hub.verify_token", "secure-token",
                "hub.challenge", "challenge-42"));

        assertEquals("subscribe", verification.mode());
        assertEquals("secure-token", verification.token());
        assertEquals("challenge-42", verification.challenge());
    }

    @Test
    void isInboundMessage_nullStructure_shouldBeFalse() {
        assertFalse(parser.isInboundMessage(null));
        assertFalse(parser.isInboundMessage(Map.of()));
        assertFalse(parser.isInboundMessage(Map.of("entry", List.of())));
        assertFalse(parser.isInboundMessage(Map.of("entry", "not-a-list")));
    }

    @Test
    void parseInboundMessage_wrongEntryShape_shouldBeEmpty() {
        Map<String, Object> payload = Map.of("entry", "oops", "changes", List.of());
        assertTrue(parser.parseInboundMessage(payload).isEmpty());
    }

    @Test
    void nestedFieldsWithUnexpectedTypes_shouldNotThrowAndReturnEmpty() {
        Map<String, Object> payload = Map.of("entry", List.of(
                Map.of("changes", List.of(
                        Map.of("value", "a-string-not-a-map")))));
        assertTrue(parser.parseInboundMessage(payload).isEmpty());
        assertFalse(parser.isInboundMessage(payload));
    }

    @Test
    void parseInboundMessage_withCaptionAndNonText_shouldFallbackToEmptyBody() {
        Map<String, Object> message = new HashMap<>();
        message.put("id", "wamid.IMG1");
        message.put("from", "5511999998888");
        message.put("type", "image");

        Map<String, Object> value = new HashMap<>();
        value.put("metadata", Map.of("phone_number_id", "1234567890"));
        value.put("messages", List.of(message));

        Map<String, Object> payload = Map.of("entry", List.of(
                Map.of("changes", List.of(Map.of("value", value)))));

        Optional<WhatsAppWebhookParser.InboundMessageData> result = parser.parseInboundMessage(payload);
        assertTrue(result.isPresent());
        assertEquals("", result.get().body());
    }

    private void assertStatus(String status, MessageStatus expected, String error) {
        Optional<WhatsAppWebhookParser.StatusData> result = parser.parseStatusUpdate(statusUpdatePayload(status));
        assertTrue(result.isPresent(), "expected status " + status + " to parse");
        assertEquals(expected, result.get().status());
        assertEquals(error, result.get().error());
    }

    private Map<String, Object> messagePayloadMissingField(String fieldToRemove) {
        Map<String, Object> message = Map.of(
                "id", "wamid.ABEGH1B2A3C4D5E6F7",
                "from", "5511999998888",
                "text", Map.of("body", "Olá, tudo bem?"));
        Map<String, Object> messageWithout = new java.util.HashMap<>(message);
        messageWithout.remove(fieldToRemove);
        return Map.of("entry", List.of(
                Map.of("changes", List.of(
                        Map.of("value", Map.of(
                                "metadata", Map.of("phone_number_id", "1234567890"),
                                "messages", List.of(messageWithout)))))));
    }

    private Map<String, Object> inboundMessagePayload() {
        return Map.of(
                "object", "whatsapp_business_account",
                "entry", List.of(Map.of(
                        "id", "1234567890",
                        "changes", List.of(Map.of(
                                "field", "messages",
                                "value", Map.of(
                                        "messaging_product", "whatsapp",
                                        "metadata", Map.of(
                                                "display_phone_number", "16505551234",
                                                "phone_number_id", "1234567890"),
                                        "contacts", List.of(Map.of(
                                                "profile", Map.of("name", "Cliente"),
                                                "wa_id", "5511999998888")),
                                        "messages", List.of(Map.of(
                                                "from", "5511999998888",
                                                "id", "wamid.ABEGH1B2A3C4D5E6F7",
                                                "timestamp", "1720000000",
                                                "text", Map.of("body", "Olá, tudo bem?"),
                                                "type", "text"))))))));
    }

    private Map<String, Object> statusUpdatePayload(String status) {
        return Map.of(
                "object", "whatsapp_business_account",
                "entry", List.of(Map.of(
                        "id", "1234567890",
                        "changes", List.of(Map.of(
                                "field", "messages",
                                "value", Map.of(
                                        "messaging_product", "whatsapp",
                                        "metadata", Map.of(
                                                "display_phone_number", "16505551234",
                                                "phone_number_id", "1234567890"),
                                        "statuses", List.of(Map.of(
                                                "id", "wamid.STATUS1",
                                                "status", status,
                                                "timestamp", "1720000001",
                                                "recipient_id", "5511999998888"))))))));
    }
}