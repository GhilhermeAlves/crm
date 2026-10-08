package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.port.output.AiMediaProvider;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsAppMediaInterpreterTest {

    private final WhatsAppProvider whatsAppProvider = mock(WhatsAppProvider.class);
    private final AiMediaProvider mediaProvider = mock(AiMediaProvider.class);
    private final OmnichannelMessageRepository messageRepository = mock(OmnichannelMessageRepository.class);
    private final WhatsAppMediaInterpreter interpreter =
            new WhatsAppMediaInterpreter(whatsAppProvider, mediaProvider, messageRepository);

    private final UUID companyId = UUID.randomUUID();
    private final Channel channel = Channel.create(companyId, ChannelType.WHATSAPP, ChannelProvider.EVOLUTION,
            "WhatsApp", "Dr. Raquel Aguiar", null, null);

    private Message inbound(MessageType type, String body) {
        Message m = Message.createInbound(companyId, UUID.randomUUID(), channel.getId(), "5534999998888",
                "Dr. Raquel Aguiar", body, "WAMID-1");
        m.markType(type);
        return m;
    }

    @Test
    void texto_naoBaixaNada() {
        assertEquals("oi", interpreter.interpret(channel, inbound(MessageType.TEXT, "oi")));
        verify(whatsAppProvider, never()).downloadMedia(any(), any(), any());
    }

    @Test
    void audio_transcreveEGravaNaMensagem() {
        Message msg = inbound(MessageType.AUDIO, "[audioMessage]");
        when(whatsAppProvider.downloadMedia("Dr. Raquel Aguiar", "WAMID-1", null))
                .thenReturn(Optional.of(new WhatsAppProvider.MediaContent(new byte[]{1, 2}, "audio/ogg", null)));
        when(mediaProvider.transcribe(any(), eq("audio/ogg"))).thenReturn("quero marcar uma limpeza");

        String text = interpreter.interpret(channel, msg);

        assertEquals("🎤 Áudio: quero marcar uma limpeza", text);
        verify(messageRepository).updateBody(msg.getId(), text);
    }

    @Test
    void imagem_comLegenda_usaResumoSemDiagnostico() {
        Message msg = inbound(MessageType.IMAGE, "meu pedido de exame");
        when(whatsAppProvider.downloadMedia(any(), any(), any()))
                .thenReturn(Optional.of(new WhatsAppProvider.MediaContent(new byte[]{1}, "image/jpeg", null)));
        when(mediaProvider.describe(any(), eq("image/jpeg"), any(), eq(WhatsAppMediaInterpreter.IMAGE_INSTRUCTION)))
                .thenReturn("Pedido de radiografia panorâmica.");

        String text = interpreter.interpret(channel, msg);

        assertTrue(text.contains("legenda: meu pedido de exame"), text);
        assertTrue(text.endsWith("Pedido de radiografia panorâmica."), text);
    }

    @Test
    void falhaNaIa_avisaQueAEquipeVaiVerificar() {
        Message msg = inbound(MessageType.DOCUMENT, "[documentMessage]");
        when(whatsAppProvider.downloadMedia(any(), any(), any()))
                .thenReturn(Optional.of(new WhatsAppProvider.MediaContent(new byte[]{1}, "application/pdf", "laudo.pdf")));
        when(mediaProvider.describe(any(), any(), any(), any())).thenThrow(new AiProviderException("HTTP 500", true));

        String text = interpreter.interpret(channel, msg);

        assertTrue(text.startsWith("📄 Documento (não foi possível ler"), text);
        verify(messageRepository).updateBody(msg.getId(), text);
    }

    @Test
    void midiaGrandeDemais_naoChamaIa() {
        Message msg = inbound(MessageType.AUDIO, "[audioMessage]");
        when(whatsAppProvider.downloadMedia(any(), any(), any())).thenReturn(Optional.of(
                new WhatsAppProvider.MediaContent(new byte[WhatsAppMediaInterpreter.MAX_MEDIA_BYTES + 1], "audio/ogg", null)));

        String text = interpreter.interpret(channel, msg);

        assertTrue(text.contains("grande demais"), text);
        verify(mediaProvider, never()).transcribe(any(), any());
    }

    @Test
    void captionOf_ignoraMarcadorDoParser() {
        assertNull(WhatsAppMediaInterpreter.captionOf("[audioMessage]"));
        assertEquals("olha isso", WhatsAppMediaInterpreter.captionOf(" olha isso "));
    }
}
