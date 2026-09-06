package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.ai.port.output.AgentAutoReplyRepository;
import com.becommerce.crm.application.ai.port.output.AgentConfigRepository;
import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.application.ai.service.AiChatFailover;
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
import com.becommerce.crm.domain.omnichannel.ConversationMode;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.omnichannel.MessageType;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Matriz de auto-resposta autônoma (Sprint 1 + Sprint 2 da portabilidade Q7 → CRM).
 *
 * <p>Sprint 1: safe defaults, proteção de loop (1 resposta por mensagem entrante),
 * cooldown, truncamento e pipeline AI → OUTBOUND pendente → envio → SENT/FAILED.
 *
 * <p>Sprint 2 (IA autônoma robusta): parâmetros de geração vindos do
 * {@link AgentConfig} (model/temperature/maxTokens), timeout/erros classificados e
 * failover limitado via {@link AiChatFailover}, contexto real da conversa
 * (Conversation/Message com INBOUND→user, OUTBOUND→assistant, janela limitada),
 * TenantContext sempre configurado e limpo.
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
    private final AiChatFailover aiChatFailover = new AiChatFailover(List.of(aiProvider));
    private final OmnichannelMessagePersister messagePersister = mock(OmnichannelMessagePersister.class);

    private WhatsAppInboundAutoReplyProcessor processor;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        processor = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository,
                conversationRepository, channelRepository, messageRepository, whatsAppProvider,
                aiChatFailover, messagePersister);

        conversation = Conversation.reconstitute(conversationId, companyId, channelId, null, from,
                ConversationStatus.OPEN, LocalDateTime.now(), 1, LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        Channel channel = Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP,
                ChannelProvider.WHATSAPP_CLOUD_API, "Principal", ChannelStatus.ACTIVE, "120000000",
                "{}", "wh-secret-ref", LocalDateTime.now(), LocalDateTime.now());
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

        when(messageRepository.findByConversation(any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));
        when(autoReplyRepository.lastAutoReplyAt(any(), any())).thenReturn(Optional.empty());
        when(autoReplyRepository.reserve(eq(companyId), eq(conversationId), eq(inboundMessageId)))
                .thenReturn(true);
        when(whatsAppProvider.send(any())).thenReturn(new WhatsAppProvider.SendResult("wamid-1"));
        when(messagePersister.persistPending(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("Atendemos de 08h às 18h."));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------ safe defaults

    @Test
    void shouldNotReplyWithoutAgentConfig() {
        when(agentConfigRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldNotReplyWhenAiDisabled() {
        config(true, false);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWhenAutoReplyNotAllowed() {
        config(false, true);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWithBlankPrompt() {
        config(true, true, "   ", 60, 1000);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWhenConversationInHumanMode() {
        conversation.takeover();
        config(true, true, "Você responde como Léo.", 60, 1000);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
        verify(autoReplyRepository, never()).reserve(any(), any(), any());
        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldReplyNormallyWhenHumanModeReleased() {
        conversation.takeover();
        conversation.releaseAutomation();
        config(true, true, "Você responde como Léo.", 60, 1000);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(messagePersister).persistPending(any());
        verify(whatsAppProvider).send(any());
    }

    @Test
    void shouldNotReplyWhenConversationBelongsToAnotherCompany() {
        config(true, true);
        Conversation other = Conversation.reconstitute(conversationId, otherCompanyId, channelId, null,
                from, ConversationStatus.OPEN, LocalDateTime.now(), 1,
                LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(other));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWhenConversationOrChannelMissing() {
        config(true, true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
    }

    // -------------------------------------------------- cooldown e loop guard

    @Test
    void shouldNotReplyWithinCooldownWindow() {
        config(true, true, "Você responde como Léo.", 60, 1000);
        when(autoReplyRepository.lastAutoReplyAt(companyId, conversationId))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(5)));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
        verify(autoReplyRepository, never()).reserve(any(), any(), any());
    }

    @Test
    void shouldReplyAfterCooldownElapsed() {
        config(true, true, "Você responde como Léo.", 60, 1000);
        when(autoReplyRepository.lastAutoReplyAt(companyId, conversationId))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(61)));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
    }

    @Test
    void shouldNotReplyTwiceForSameInboundMessage() {
        config(true, true);
        when(autoReplyRepository.reserve(companyId, conversationId, inboundMessageId))
                .thenReturn(false);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider, never()).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
        verify(autoReplyRepository).reserve(companyId, conversationId, inboundMessageId);
    }

    // ------------------------------------------------- parâmetros de geração

    @Test
    void shouldPassGenerationParamsFromAgentConfig() {
        config(true, true, "Você responde como Léo.", "gpt-4o", 0.7, 300, 60, 1000);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        ArgumentCaptor<AiProvider.ChatRequest> captor = ArgumentCaptor.forClass(AiProvider.ChatRequest.class);
        verify(aiProvider).chatWithTools(captor.capture());
        AiProvider.GenerationParams params = captor.getValue().params();
        assertEquals("gpt-4o", params.model());
        assertEquals(0.7, params.temperature());
        assertEquals(300, params.maxTokens());
        assertEquals(companyId, captor.getValue().companyId());
    }

    @Test
    void shouldUseProviderDefaultsWhenGenerationParamsAreNull() {
        config(true, true, "Você responde como Léo.", null, null, null, 60, 1000);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        ArgumentCaptor<AiProvider.ChatRequest> captor = ArgumentCaptor.forClass(AiProvider.ChatRequest.class);
        verify(aiProvider).chatWithTools(captor.capture());
        AiProvider.GenerationParams params = captor.getValue().params();
        assertNull(params.model());
        assertNull(params.temperature());
        assertNull(params.maxTokens());
    }

    // -------------------------------------------------------------- pipeline IA

    @Test
    void shouldGenerateReplyPersistPendingSendAndMarkSent() {
        config(true, true);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
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
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("0123456789ABCDEF"));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister).persistPending(argThat(m -> "0123456789AB".equals(m.getBody())));
        verify(whatsAppProvider).send(argThat(r -> "0123456789AB".equals(r.body())));
    }

    @Test
    void shouldNotReplyWhenAiReturnsEmptyContent() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content(""));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldNotReplyWhenAiReturnsBlankContent() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("   "));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldNotReplyWhenAiReturnsNullContent() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenReturn(new AiProvider.ChatResult(null, List.of()));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldNotPersistOrSendWhenAiFails() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
        verify(messagePersister, never()).markFailed(any(), any(), anyString());
    }

    @Test
    void shouldNotPersistOrSendWhenAiTimesOut() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldMarkFailedWhenProviderSendThrows() {
        config(true, true);
        when(whatsAppProvider.send(any()))
                .thenThrow(new OmnichannelProviderException("provider down"));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister).persistPending(any());
        verify(messagePersister).markFailed(any(), eq(conversationId), eq("provider down"));
        verify(messagePersister, never()).markSent(any(), any(), anyString());
    }

    // ------------------------------------------------------------- failover

    @Test
    void shouldFallbackToSecondaryProviderAndSendOnce() {
        config(true, true);
        AiProvider fallback = mock(AiProvider.class);
        when(fallback.providerName()).thenReturn("FALLBACK");
        when(fallback.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("resposta do fallback"));
        when(aiProvider.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));

        processor = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository,
                conversationRepository, channelRepository, messageRepository, whatsAppProvider,
                new AiChatFailover(List.of(aiProvider, fallback)), messagePersister);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(fallback).chatWithTools(any());
        // Uma única resposta enviada — nunca duas (proteção contra duplicação no fallback).
        verify(messagePersister).persistPending(argThat(m -> "resposta do fallback".equals(m.getBody())));
        verify(whatsAppProvider).send(argThat(r -> "resposta do fallback".equals(r.body())));
    }

    @Test
    void shouldNotSendWhenAllProvidersFail() {
        config(true, true);
        AiProvider fallback = mock(AiProvider.class);
        when(fallback.providerName()).thenReturn("FALLBACK");
        when(aiProvider.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));
        when(fallback.chatWithTools(any())).thenThrow(new AiProviderException("provider down", true));

        processor = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository,
                conversationRepository, channelRepository, messageRepository, whatsAppProvider,
                new AiChatFailover(List.of(aiProvider, fallback)), messagePersister);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(fallback).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void shouldNotFallbackOnNonRecoverableError() {
        config(true, true);
        AiProvider fallback = mock(AiProvider.class);
        when(fallback.providerName()).thenReturn("FALLBACK");
        when(aiProvider.chatWithTools(any()))
                .thenThrow(new AiProviderException("config inválida", false));
        when(aiProvider.providerName()).thenReturn("PRIMARY");

        processor = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository,
                conversationRepository, channelRepository, messageRepository, whatsAppProvider,
                new AiChatFailover(List.of(aiProvider, fallback)), messagePersister);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(fallback, never()).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
        verify(whatsAppProvider, never()).send(any());
    }

    // -------------------------------------------------------------- histórico

    @Test
    void shouldBuildHistoryFromConversationMessages() {
        config(true, true);
        Message inbound = Message.reconstitute(UUID.randomUUID(), companyId, conversationId, channelId,
                MessageDirection.INBOUND, from, "120000000", MessageType.TEXT, "oi anterior",
                MessageStatus.SENT, "wamid-00", UUID.randomUUID(), null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        Message outbound = Message.reconstitute(UUID.randomUUID(), companyId, conversationId, channelId,
                MessageDirection.OUTBOUND, "120000000", from, MessageType.TEXT, "resposta anterior",
                MessageStatus.SENT, "wamid-01", UUID.randomUUID(), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now());
        when(messageRepository.findByConversation(conversationId, 0, 20))
                .thenReturn(PageResponse.of(List.of(inbound, outbound), 0, 20, 2));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(argThat(req -> {
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

    @Test
    void shouldNotDuplicateCurrentInboundMessage() {
        config(true, true);
        // A mensagem entrante em processamento JÁ está persistida no histórico;
        // ela deve entrar UMA única vez (como última mensagem user), nunca duas.
        Message current = Message.reconstitute(inboundMessageId, companyId, conversationId, channelId,
                MessageDirection.INBOUND, from, "120000000", MessageType.TEXT, body,
                MessageStatus.SENT, "wamid-current", UUID.randomUUID(), null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(messageRepository.findByConversation(conversationId, 0, 20))
                .thenReturn(PageResponse.of(List.of(current), 0, 20, 1));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(argThat(req ->
                req.messages().stream().filter(m -> "user".equals(m.role()) && body.equals(m.content())).count() == 1));
    }

    @Test
    void shouldLimitHistoryByTokenBudgetKeepingMostRecentFirst() {
        config(true, true, "Você responde como Léo.", null, null, 8, 60, 1000);
        // maxTokens=8 → orçamento de histórico = 8 * 2 * 4 = 64 caracteres.
        List<Message> history = List.of(
                message(MessageDirection.OUTBOUND, "x".repeat(120), "wamid-1"),
                message(MessageDirection.INBOUND, "mensagem recente", "wamid-2"),
                message(MessageDirection.INBOUND, "mais recente ainda", "wamid-3"));
        when(messageRepository.findByConversation(conversationId, 0, 20))
                .thenReturn(PageResponse.of(history, 0, 20, history.size()));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(argThat(req -> {
            List<AiProvider.ChatMessage> messages = req.messages();
            // A mais antiga (120 chars) estoura o orçamento e deve ser podada.
            assertFalse(messages.stream().anyMatch(m -> m.content() != null
                    && m.content().length() == 120));
            // As mais recentes entram (a janela mantém ao menos a última).
            assertTrue(messages.stream().anyMatch(m -> "mais recente ainda".equals(m.content())));
            assertTrue(messages.stream().anyMatch(m -> body.equals(m.content())));
            return true;
        }));
    }

    @Test
    void shouldSkipBlankHistoryMessages() {
        config(true, true);
        Message blank = Message.reconstitute(UUID.randomUUID(), companyId, conversationId, channelId,
                MessageDirection.INBOUND, from, "120000000", MessageType.TEXT, "   ",
                MessageStatus.SENT, "wamid-blank", UUID.randomUUID(), null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(messageRepository.findByConversation(conversationId, 0, 20))
                .thenReturn(PageResponse.of(List.of(blank), 0, 20, 1));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(argThat(req -> {
            List<AiProvider.ChatMessage> messages = req.messages();
            assertEquals(2, messages.size(), "system + mensagem atual; histórico em branco ignorado");
            return true;
        }));
    }

    // ------------------------------------------------------------ tenant context

    @Test
    void shouldSetTenantContextDuringProcessing() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenAnswer(inv -> {
            assertEquals(companyId, TenantContext.getCompanyId(),
                    "TenantContext deve estar configurado durante a chamada à IA");
            return AiProvider.ChatResult.content("Ok");
        });

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        assertNull(TenantContext.getCompanyId(), "TenantContext deve ser limpo após o processamento");
    }

    @Test
    void shouldClearTenantContextEvenWhenNoConfig() {
        when(agentConfigRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        assertNull(TenantContext.getCompanyId());
    }

    @Test
    void shouldClearTenantContextEvenWhenAiFails() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        assertNull(TenantContext.getCompanyId());
    }

    // --------------------------------------------------------------- helpers

    private Message message(MessageDirection direction, String text, String externalId) {
        return Message.reconstitute(UUID.randomUUID(), companyId, conversationId, channelId,
                direction, from, "120000000", MessageType.TEXT, text,
                MessageStatus.SENT, externalId, UUID.randomUUID(), null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private void config(boolean aiEnabled, boolean allowAutoReply) {
        config(aiEnabled, allowAutoReply, "Você responde como Léo.", 60, 1000);
    }

    private void config(boolean aiEnabled, boolean allowAutoReply, String prompt,
                        int cooldownMinutes, int maxChars) {
        config(aiEnabled, allowAutoReply, prompt, null, null, null, cooldownMinutes, maxChars);
    }

    private void config(boolean aiEnabled, boolean allowAutoReply, String prompt,
                        String model, Double temperature, Integer maxTokens,
                        int cooldownMinutes, int maxChars) {
        when(agentConfigRepository.findByCompanyId(companyId)).thenReturn(Optional.of(
                AgentConfig.reconstitute(UUID.randomUUID(), companyId, aiEnabled, allowAutoReply,
                        prompt, model, temperature, maxTokens, cooldownMinutes, maxChars,
                        LocalDateTime.now(), LocalDateTime.now())));
    }
}