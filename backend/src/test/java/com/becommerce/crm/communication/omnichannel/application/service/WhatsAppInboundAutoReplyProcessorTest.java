package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.port.output.AgentAutoReplyRepository;
import com.becommerce.crm.automation.ai.application.port.output.AgentConfigRepository;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.service.AiChatFailover;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppEventPublisher;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelStatus;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.communication.omnichannel.domain.ConversationStatus;
import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageDirection;
import com.becommerce.crm.communication.omnichannel.application.event.WhatsAppSendEvent;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import com.becommerce.crm.shared.tenant.context.TenantContext;
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
import static org.mockito.Mockito.doThrow;
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
    private final WhatsAppEventPublisher eventPublisher = mock(WhatsAppEventPublisher.class);
    private final AiProvider aiProvider = mock(AiProvider.class);
    private final AiChatFailover aiChatFailover = new AiChatFailover(List.of(aiProvider));
    private final OmnichannelMessagePersister messagePersister = mock(OmnichannelMessagePersister.class);

    private WhatsAppInboundAutoReplyProcessor processor;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        processor = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository,
                conversationRepository, channelRepository, messageRepository,
                aiChatFailover, messagePersister, eventPublisher);

        conversation = Conversation.reconstitute(conversationId, companyId, channelId, null, from,
                ConversationStatus.OPEN, LocalDateTime.now(), 1, LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        Channel channel = Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP,
                ChannelProvider.EVOLUTION, "Principal", ChannelStatus.ACTIVE, "120000000",
                "{}", "wh-secret-ref", LocalDateTime.now(), LocalDateTime.now());
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

        when(messageRepository.findRecentByConversation(any(), anyInt())).thenReturn(List.of());
        when(autoReplyRepository.lastAutoReplyAt(any(), any())).thenReturn(Optional.empty());
        when(autoReplyRepository.reserve(eq(companyId), eq(conversationId), eq(inboundMessageId)))
                .thenReturn(true);
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
    }

    @Test
    void shouldReplyNormallyWhenHumanModeReleased() {
        conversation.takeover();
        conversation.releaseAutomation();
        config(true, true, "Você responde como Léo.", 60, 1000);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(messagePersister).persistPending(any());
        verify(eventPublisher).publishSend(any());
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
    void shouldGenerateReplyPersistPendingPublishSend() {
        config(true, true);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(messagePersister).persistPending(argThat(m ->
                m.getDirection() == MessageDirection.OUTBOUND
                        && m.getCompanyId().equals(companyId)
                        && m.getConversationId().equals(conversationId)
                        && "Atendemos de 08h às 18h.".equals(m.getBody())));
        verify(eventPublisher).publishSend(argThat(e ->
                e.companyId().equals(companyId)
                        && e.conversationId().equals(conversationId)
                        && e.channelId().equals(channelId)
                        && e.to().equals(from)
                        && "Atendemos de 08h às 18h.".equals(e.body())
                        && e.followUpId() == null));
        // o envio efetivo/status (markSent/markFailed) passou a ser do consumer de sender.
        verify(messagePersister, never()).markSent(any(), any(), any());
    }

    @Test
    void shouldTruncateReplyToMaxChars() {
        config(true, true, "Você responde como Léo.", 60, 12);
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("0123456789ABCDEF"));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister).persistPending(argThat(m -> "0123456789AB".equals(m.getBody())));
        verify(eventPublisher).publishSend(argThat(e -> "0123456789AB".equals(e.body())));
    }

    @Test
    void shouldNotReplyWhenAiReturnsEmptyContent() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content(""));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWhenAiReturnsBlankContent() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("   "));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotReplyWhenAiReturnsNullContent() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenReturn(new AiProvider.ChatResult(null, List.of()));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldNotPersistOrSendWhenAiFails() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister, never()).persistPending(any());
        verify(messagePersister, never()).markFailed(any(), any(), anyString());
    }

    @Test
    void shouldNotPersistOrSendWhenAiTimesOut() {
        config(true, true);
        when(aiProvider.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void shouldMarkFailedWhenPublishSendThrows() {
        config(true, true);
        doThrow(new IllegalStateException("broker down"))
                .when(eventPublisher).publishSend(any());

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(messagePersister).persistPending(any());
        verify(messagePersister).markFailed(any(), eq(conversationId), eq("broker down"));
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
                conversationRepository, channelRepository, messageRepository,
                new AiChatFailover(List.of(aiProvider, fallback)), messagePersister, eventPublisher);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(fallback).chatWithTools(any());
        // Uma única resposta enviada — nunca duas (proteção contra duplicação no fallback).
        verify(messagePersister).persistPending(argThat(m -> "resposta do fallback".equals(m.getBody())));
        verify(eventPublisher).publishSend(argThat(e -> "resposta do fallback".equals(e.body())));
    }

    @Test
    void shouldNotSendWhenAllProvidersFail() {
        config(true, true);
        AiProvider fallback = mock(AiProvider.class);
        when(fallback.providerName()).thenReturn("FALLBACK");
        when(aiProvider.chatWithTools(any())).thenThrow(new AiProviderException("timeout", true));
        when(fallback.chatWithTools(any())).thenThrow(new AiProviderException("provider down", true));

        processor = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository,
                conversationRepository, channelRepository, messageRepository,
                new AiChatFailover(List.of(aiProvider, fallback)), messagePersister, eventPublisher);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(fallback).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
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
                conversationRepository, channelRepository, messageRepository,
                new AiChatFailover(List.of(aiProvider, fallback)), messagePersister, eventPublisher);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(any());
        verify(fallback, never()).chatWithTools(any());
        verify(messagePersister, never()).persistPending(any());
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
        when(messageRepository.findRecentByConversation(conversationId, 20)).thenReturn(List.of(inbound, outbound));

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
        when(messageRepository.findRecentByConversation(conversationId, 20)).thenReturn(List.of(current));

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
        when(messageRepository.findRecentByConversation(conversationId, 20)).thenReturn(history);

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
        when(messageRepository.findRecentByConversation(conversationId, 20)).thenReturn(List.of(blank));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(argThat(req -> {
            List<AiProvider.ChatMessage> messages = req.messages();
            assertEquals(3, messages.size(), "prompt + contexto + mensagem atual; histórico em branco ignorado");
            return true;
        }));
    }

    // ------------------------------------------------------------ humanização

    @Test
    void shouldSendSenderNameAndCurrentDateAsContext() {
        config(true, true);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body, "Maria Souza");

        verify(aiProvider).chatWithTools(argThat(req -> {
            AiProvider.ChatMessage facts = req.messages().get(1);
            assertEquals("system", facts.role());
            assertTrue(facts.content().contains("\"Maria Souza\""), facts.content());
            assertTrue(facts.content().contains("America/Sao_Paulo"), facts.content());
            return true;
        }));
    }

    @Test
    void legacyAgent_keepsPromptVerbatimAsFirstSystemMessage() {
        config(true, true);

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(argThat(req -> {
            AiProvider.ChatMessage first = req.messages().get(0);
            return "system".equals(first.role()) && "Você responde como Léo.".equals(first.content());
        }));
    }

    @Test
    void structuredAgent_sendsIdentityAndBehaviorInsteadOfLegacyPrompt() {
        AgentConfig agent = AgentConfig.reconstitute(UUID.randomUUID(), companyId, true, true,
                "Prompt antigo", null, null, null, 60, 1000, LocalDateTime.now(), LocalDateTime.now())
                .withProfile(new com.becommerce.crm.automation.ai.domain.AgentIdentity("Ana Laura",
                                "Assistente virtual", "Você é Ana Laura."),
                        new com.becommerce.crm.automation.ai.domain.AgentBehavior("Agendar consultas",
                                "Acolhedor", List.of("Não inventar informações."), List.of()),
                        false, false);
        when(agentConfigRepository.findByCompanyId(companyId)).thenReturn(Optional.of(agent));

        processor.processInbound(companyId, conversationId, inboundMessageId, from, body);

        verify(aiProvider).chatWithTools(argThat(req -> {
            String first = req.messages().get(0).content();
            return first.contains("Nome: Ana Laura") && first.contains("- Não inventar informações.")
                    && !first.contains("Prompt antigo");
        }));
    }

    @Test
    void humanTransferTool_isOfferedOnlyWhenEnabled() {
        AgentConfig agent = AgentConfig.reconstitute(UUID.randomUUID(), companyId, true, true,
                "Prompt", null, null, null, 60, 1000, LocalDateTime.now(), LocalDateTime.now())
                .withProfile(null, null, false, true);
        when(agentConfigRepository.findByCompanyId(companyId)).thenReturn(Optional.of(agent));
        com.becommerce.crm.automation.ai.application.agent.AgentContextBuilder builder =
                com.becommerce.crm.automation.ai.application.agent.AgentContextBuilder.withoutCrmContext(
                        new com.becommerce.crm.automation.ai.application.agent.tool.AgentToolbox(
                                List.of(new HumanTransferToolProvider(conversationRepository))));
        AiProvider.ToolCall call = new AiProvider.ToolCall("t1", "transferir_para_humano",
                java.util.Map.of("motivo", "pediu atendente"));
        when(aiProvider.chatWithTools(any()))
                .thenReturn(AiProvider.ChatResult.withToolCalls(List.of(call)))
                .thenReturn(AiProvider.ChatResult.content("Vou te passar para a equipe."));

        new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository, conversationRepository,
                channelRepository, messageRepository, aiChatFailover, messagePersister, eventPublisher, 0L, null,
                builder).processInbound(companyId, conversationId, inboundMessageId, from, body);

        assertTrue(conversation.isInHumanMode(), "a ferramenta deve colocar a conversa em modo humano");
        verify(conversationRepository).save(conversation);
        verify(eventPublisher).publishSend(argThat(e -> e.body().startsWith("Vou te passar")));
    }

    @Test
    void shouldSkipWhenNewerInboundArrivesDuringDebounce() {
        config(true, true);
        WhatsAppInboundAutoReplyProcessor debounced = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository,
                autoReplyRepository, conversationRepository, channelRepository, messageRepository,
                aiChatFailover, messagePersister, eventPublisher, 1L);
        Message current = message(MessageDirection.INBOUND, "primeira", "wamid-a");
        when(messageRepository.findById(inboundMessageId)).thenReturn(Optional.of(current));
        when(messageRepository.existsInboundAfter(eq(conversationId), any())).thenReturn(true);

        debounced.processInbound(companyId, conversationId, inboundMessageId, from, "primeira");

        verify(autoReplyRepository, never()).reserve(any(), any(), any());
        verify(aiProvider, never()).chatWithTools(any());
    }

    @Test
    void shouldReplyAfterDebounceWhenNoNewerInbound() {
        config(true, true);
        WhatsAppInboundAutoReplyProcessor debounced = new WhatsAppInboundAutoReplyProcessor(agentConfigRepository,
                autoReplyRepository, conversationRepository, channelRepository, messageRepository,
                aiChatFailover, messagePersister, eventPublisher, 1L);
        Message current = message(MessageDirection.INBOUND, "última", "wamid-b");
        when(messageRepository.findById(inboundMessageId)).thenReturn(Optional.of(current));
        when(messageRepository.existsInboundAfter(eq(conversationId), any())).thenReturn(false);

        debounced.processInbound(companyId, conversationId, inboundMessageId, from, "última");

        verify(aiProvider).chatWithTools(any());
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

    // ------------------------------------------------------- agenda, mídia e voz

    private WhatsAppInboundAutoReplyProcessor fullProcessor(WhatsAppMediaInterpreter media,
                                                           WhatsAppSchedulingTools tools) {
        return new WhatsAppInboundAutoReplyProcessor(agentConfigRepository, autoReplyRepository,
                conversationRepository, channelRepository, messageRepository, aiChatFailover, messagePersister,
                eventPublisher, 0L, media, tools);
    }

    @Test
    void shouldRunSchedulingToolsAndAnswerWithTheirResult() {
        config(true, true);
        WhatsAppSchedulingTools tools = mock(WhatsAppSchedulingTools.class);
        when(tools.bookableTypes(companyId)).thenReturn(List.of(
                mock(com.becommerce.crm.sales.scheduling.domain.AppointmentType.class)));
        when(tools.definitions()).thenReturn(List.of(new AiProvider.ToolDefinition("consultar_horarios_livres",
                "x", java.util.Map.of())));
        when(tools.guidance(any())).thenReturn("Agenda online disponível.");
        AiProvider.ToolCall call = new AiProvider.ToolCall("c1", "consultar_horarios_livres",
                java.util.Map.of("tipo_consulta", "Avaliação"));
        when(tools.execute(any(), eq(call))).thenReturn("Horários livres: qua 15/10 14:00");
        when(aiProvider.chatWithTools(any()))
                .thenReturn(AiProvider.ChatResult.withToolCalls(List.of(call)))
                .thenReturn(AiProvider.ChatResult.content("Tenho quarta às 14h. Pode ser?"));

        fullProcessor(null, tools).processInbound(companyId, conversationId, inboundMessageId, from, body, "Maria");

        verify(tools).execute(argThat(c -> companyId.equals(c.companyId()) && "Maria".equals(c.senderName())),
                eq(call));
        verify(aiProvider, org.mockito.Mockito.atLeastOnce()).chatWithTools(argThat(req -> req.messages().stream().anyMatch(m ->
                "tool".equals(m.role()) && "c1".equals(m.toolCallId())
                        && m.content().contains("qua 15/10 14:00"))));
        verify(eventPublisher).publishSend(argThat(e -> e.body().startsWith("Tenho quarta") && !e.voice()));
    }

    @Test
    void audioRecebido_ehTranscritoERespondidoEmVozNoModoEspelho() {
        config(true, true);
        Message audio = Message.reconstitute(inboundMessageId, companyId, conversationId, channelId,
                MessageDirection.INBOUND, from, "120000000", MessageType.AUDIO, "[audioMessage]",
                MessageStatus.SENT, "wamid-audio", UUID.randomUUID(), null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(messageRepository.findById(inboundMessageId)).thenReturn(Optional.of(audio));
        WhatsAppMediaInterpreter media = mock(WhatsAppMediaInterpreter.class);
        when(media.interpret(any(), eq(audio))).thenReturn("🎤 Áudio: queria marcar uma limpeza");

        fullProcessor(media, null).processInbound(companyId, conversationId, inboundMessageId, from, "[audioMessage]");

        verify(aiProvider).chatWithTools(argThat(req -> {
            AiProvider.ChatMessage last = req.messages().get(req.messages().size() - 1);
            return "user".equals(last.role()) && last.content().contains("queria marcar uma limpeza");
        }));
        verify(eventPublisher).publishSend(argThat(WhatsAppSendEvent::voice));
    }

    @Test
    void shouldReplyWithVoice_respeitaOModo() {
        assertTrue(WhatsAppInboundAutoReplyProcessor.shouldReplyWithVoice(
                com.becommerce.crm.automation.ai.domain.VoiceReplyMode.MIRROR, true));
        assertFalse(WhatsAppInboundAutoReplyProcessor.shouldReplyWithVoice(
                com.becommerce.crm.automation.ai.domain.VoiceReplyMode.MIRROR, false));
        assertTrue(WhatsAppInboundAutoReplyProcessor.shouldReplyWithVoice(
                com.becommerce.crm.automation.ai.domain.VoiceReplyMode.ALWAYS, false));
        assertFalse(WhatsAppInboundAutoReplyProcessor.shouldReplyWithVoice(
                com.becommerce.crm.automation.ai.domain.VoiceReplyMode.NEVER, true));
    }

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