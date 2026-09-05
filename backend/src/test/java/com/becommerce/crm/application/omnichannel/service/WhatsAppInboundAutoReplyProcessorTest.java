package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.ai.port.output.AgentAutoReplyRepository;
import com.becommerce.crm.application.ai.port.output.AgentConfigRepository;
import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.ai.AgentConfig;
import com.becommerce.crm.domain.ai.AiProviderException;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.ChannelProvider;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.ChannelType;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Matriz de auto-resposta autônoma (Sprint 1 da portabilidade Q7 → CRM).
 *
 * <p>Cobre os safe defaults, a proteção de loop (1 resposta por mensagem
 * entrante mesmo reprocessada), cooldown, truncamento e o pipeline
 * AI → OUTBOUND pendente → envio → SENT/FAILED.</p>
 */
class WhatsAppInboundAutoReplyProcessorTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID otherCompanyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID inboundMessageId = UUID.randomUUID();
    private final String from = "5511999999999";
    private final String body = "Qual o horário de atendimento?";

    private final AgentConfigRepository agentConfigRepository = mock(AgentConfigRepository.class);
    private final AgentAutoReplyRepository autoReplyRepository = mock(AgentAutoReplyRepository.class);
    private final OmnichannelConversationRepository conversationRepository =
            mock(OmnichannelConversationRepository.class);
    private final OmnichannelChannelRepository channelRepository = mock(OmnichannelChannelRepository.class);
    private final OmnichannelMessageRepository messageRepository = mock(OmnichannelMessageRepository.class);
    private final WhatsAppProvider whatsAppProvider = mock(WhatsAppProvider.class);
    private final AiProvider aiProvider = mock(AiProvider.class);
    private final OmnichannelMessagePersister messagePersister = mock(OmnichannelMessagePersister.class);

    private WhatsAppInboundAutoReplyProcessor processor;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        processor = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository,
                conversationRepository, channelRepository, messageRepository, whatsAppProvider,
                aiProvider, messagePersister);

        conversation = Conversation.reconstitute(conversationId, companyId, channelId, null, from,
                ConversationStatus.OPEN, LocalDateTime.now(), 1, LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        Channel channel = Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP,
                ChannelProvider.WHATSAPP_CLOUD_API, "Principal", ChannelStatus.ACTIVE, "120000000",
                "{}", "wh-secret-ref", LocalDateTime.now(), LocalDateTime.now());
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

        when(messageRepository.findByConversation(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));
        when(autoReplyRepository.lastAutoReplyAt(any(), any())).thenReturn(Optional.empty());
        when(autoReplyRepository.reserve(eq(companyId), eq(conversationId), eq(inboundMessageId)))
                .thenReturn(true);
        when(whatsAppProvider.send(any())).thenReturn(new WhatsAppProvider.SendResult("wamid-1"));
        when(messagePersister.persistPending(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------ safe defaults

    @Test
    void shouldNotReplyWithoutAgentConfig() {
        when(agentConfigRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());
        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldNotReplyWhenAiDisabled() {
        config(true, false);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWhenAutoReplyNotAllowed() {
        config(false, true);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWithBlankPrompt() {
        config(true, true, "   ", 60, 1000);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWhenConversationBelongsToAnotherCompany() {
        config(true, true);
        Conversation other = Conversation.reconstitute(conversationId, otherCompanyId, channelId, null,
                from, ConversationStatus.OPEN, LocalDateTime.now(), 1,
                LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(other));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWhenConversationOrChannelMissing() {
        config(true, true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());
    }

    // -------------------------------------------------- cooldown e loop guard

    @Test
    void shouldNotReplyWithinCooldownWindow() {
        config(true, true, "Você responde como Léo.", 60, 1000);
        when(autoReplyRepository.lastAutoReplyAt(companyId, conversationId))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(5)));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());
        verify(autoReplyRepository, never()).reserve(any(), any(), any());
    }

    @Test
    void shouldNotReplyAfterCooldownWindow() {
        config(true, true, "Você responde como Léo.", 60, 1000);
        when(autoReplyRepository.lastAutoReplyAt(companyId, conversationId))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(61)));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chat(any());
    }

    @Test
    void shouldNotReplyTwiceForSameInboundMessage() {
        config(true, true);
        when(autoReplyRepository.reserve(companyId, conversationId, inboundMessageId))
                .thenReturn(false);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chat(any());
        verify(messagePersister, never()).persistPending(any());
    }

    // -------------------------------------------------------------- pipeline IA

    @Test
    void shouldGenerateReplyPersistPendingSendAndMarkSent() {
        config(true, true);
        when(aiProvider.chat(any())).thenReturn("Atendemos de 08h às 18h.");

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chat(any());
        verify(messagePersister).persistPending(argThat(m ->
                m.getDirection() == MessageDirection.OUTBOUND
                        && m.getCompanyId().equals(companyId)
                        && m.getConversationId().equals(conversationId)
                        && "Atendemos de 08h às 18h.".equals(m.getBody())));
        verify(whatsAppProvider).send(argThat(r ->
                r.companyId().equals(companyId)
                        && r.channelId().equals(channelId)
                        && r.to().equals(from)
                        && "Atendemos de 08h às 18h.".equals(r.body())));
        verify(messagePersister).markSent(any(), eq(conversationId), eq("wamid-1"));
    }

    @Test
    void shouldTruncateReplyToMaxChars() {
        config(true, true, "Você responde como Léo.", 60, 12);
        when(aiProvider.chat(any())).thenReturn("0123456789ABCDEF");

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister).persistPending(argThat(m -> "0123456789AB".equals(m.getBody())));
        verify(whatsAppProvider).send(argThat(r -> "0123456789AB".equals(r.body())));
    }

    @Test
    void shouldNotReplyWhenAiReturnsEmptyContent() {
        config(true, true);
        when(aiProvider.chat(any())).thenReturn("");

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldNotPersistOrSendWhenAiFails() {
        config(true, true);
        when(aiProvider.chat(any())).thenThrow(new AiProviderException("timeout"));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
        verify(messagePersister, never()).markFailed(any(), any(), anyString());
    }

    @Test
    void shouldMarkFailedWhenProviderSendThrows() {
        config(true, true);
        when(aiProvider.chat(any())).thenReturn("Ok!");
        when(whatsAppProvider.send(any()))
                .thenThrow(new OmnichannelProviderException("provider down"));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister).persistPending(any());
        verify(messagePersister).markFailed(any(), eq(conversationId), eq("provider down"));
        verify(messagePersister, never()).markSent(any(), any(), anyString());
    }

    // -------------------------------------------------------------- histórico

    @Test
    void shouldBuildHistoryFromConversationMessages() {
        config(true, true);
        Message inbound = Message.createInbound(companyId, conversationId, channelId, from,
                "120000000", "oi anterior", "wamid-00");
        Message outbound = Message.createOutbound(companyId, conversationId, channelId,
                "120000000", from, "resposta anterior", UUID.randomUUID());
        when(messageRepository.findByConversation(conversationId, 0, WhatsAppInboundAutoReplyProcessorTest.HISTORY_LIMIT))
                .thenReturn(PageResponse.of(List.of(inbound, outbound), 0, 20, 2));
        when(aiProvider.chat(any())).thenReturn("R");

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chat(argThat(req -> {
            List<AiProvider.ChatMessage> messages = req.messages();
            assertTrue(messages.stream().anyMatch(m ->
                    "system".equals(m.role()) && "Você responde como Léo.".equals(m.content())));
            assertTrue(messages.stream().anyMatch(m ->
                    "user".equals(m.role()) && "oi anterior".equals(m.content())));
            assertTrue(messages.stream().anyMatch(m ->
                    "assistant".equals(m.role()) && "resposta anterior".equals(m.content())));
            assertTrue(messages.stream().anyMatch(m ->
                    "user".equals(m.role()) && body.equals(m.content())));
            return true;
        }));
    }

    // --------------------------------------------------------------- helpers

    private void config(boolean aiEnabled, boolean allowAutoReply) {
        config(aiEnabled, allowAutoReply, "Você responde como Léo.", 60, 1000);
    }

    private void config(boolean aiEnabled, boolean allowAutoReply, String prompt,
                        int cooldownMinutes, int maxChars) {
        when(agentConfigRepository.findByCompanyId(companyId)).thenReturn(Optional.of(
                AgentConfig.reconstitute(UUID.randomUUID(), companyId, aiEnabled, allowAutoReply,
                        prompt, cooldownMinutes, maxChars, LocalDateTime.now(), LocalDateTime.now())));
    }

    private static final int HISTORY_LIMIT = 20;
}