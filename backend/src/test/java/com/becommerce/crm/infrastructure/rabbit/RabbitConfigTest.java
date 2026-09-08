package com.becommerce.crm.infrastructure.rabbit;

import com.becommerce.crm.application.followup.event.FollowUpExecutionEvent;
import com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Round-trip do converter JSON tipado SEM broker: publica {@code toMessage}
 * (grava {@code __TypeId__}) e desserializa {@code fromMessage} — exercitando
 * exatamente o caminho de trust do {@code DefaultJackson2JavaTypeMapper} que os
 * consumers usam. O IT de fila real ({@code RabbitMessagingFlowIT}) cobre o
 * transporte completo; este teste trava o detalhe de infraestrutura mais fácil
 * de regredir (pacotes confiáveis).
 */
class RabbitConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void whatsAppSendEvent_shouldRoundTripTypedJson() {
        MessageConverter converter = new RabbitConfig().messageConverter(objectMapper);

        WhatsAppSendEvent sent = WhatsAppSendEvent.ofFollowUp(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "+5511999998888", "olá", UUID.randomUUID());

        Message message = converter.toMessage(sent, new MessageProperties());
        Object converted = converter.fromMessage(message);

        assertEquals(sent, converted);
        assertNotNull(converted);
    }

    @Test
    void followUpExecutionEvent_shouldRoundTripTypedJson() {
        MessageConverter converter = new RabbitConfig().messageConverter(objectMapper);

        FollowUpExecutionEvent sent = FollowUpExecutionEvent.of(UUID.randomUUID(), UUID.randomUUID());
        Message message = converter.toMessage(sent, new MessageProperties());
        Object converted = converter.fromMessage(message);

        assertEquals(sent, converted);
        assertNotNull(converted);
    }
}