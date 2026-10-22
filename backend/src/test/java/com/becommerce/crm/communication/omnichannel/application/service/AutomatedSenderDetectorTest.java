package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageDirection;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomatedSenderDetectorTest {

    @Test
    void shouldDetectAutomatedPhrasesIgnoringAccentsAndCase() {
        assertTrue(AutomatedSenderDetector.detect("Esta é uma MENSAGEM AUTOMÁTICA.", List.of()).isPresent());
        assertTrue(AutomatedSenderDetector.detect("Olá, sou a assistente virtual da loja", List.of()).isPresent());
        assertTrue(AutomatedSenderDetector.detect("Digite o número da opção desejada", List.of()).isPresent());
    }

    @Test
    void shouldDetectNumberedMenu() {
        String menu = """
                Como podemos ajudar?
                1 - Orçamento
                2 - Financeiro
                3) Falar com atendente""";
        assertTrue(AutomatedSenderDetector.detect(menu, List.of()).isPresent());
    }

    @Test
    void shouldDetectKeycapEmojiMenu() {
        assertTrue(AutomatedSenderDetector.detect("1️⃣ Vendas\n2️⃣ Suporte", List.of()).isPresent());
    }

    @Test
    void shouldDetectSameMessageRepeatedAfterOurReply() {
        String greeting = "Olá! Obrigado por entrar em contato com a Loja X.";
        List<Message> recent = List.of(
                message(MessageDirection.INBOUND, greeting),
                message(MessageDirection.OUTBOUND, "Oi! Quer agendar uma avaliação?"),
                message(MessageDirection.INBOUND, greeting));
        assertTrue(AutomatedSenderDetector.detect(greeting, recent).isPresent());
    }

    @Test
    void shouldTreatOrdinaryPatientMessagesAsHuman() {
        assertFalse(AutomatedSenderDetector.detect("Oi, queria marcar uma limpeza para sexta", List.of()).isPresent());
        assertFalse(AutomatedSenderDetector.detect("ok", List.of(
                message(MessageDirection.INBOUND, "ok"),
                message(MessageDirection.OUTBOUND, "Combinado!"))).isPresent());
        assertFalse(AutomatedSenderDetector.detect(null, List.of()).isPresent());
    }

    private static Message message(MessageDirection direction, String body) {
        return Message.reconstitute(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                direction, "5534999990000", "5534988880000", MessageType.TEXT, body, MessageStatus.SENT,
                null, UUID.randomUUID(), null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }
}
