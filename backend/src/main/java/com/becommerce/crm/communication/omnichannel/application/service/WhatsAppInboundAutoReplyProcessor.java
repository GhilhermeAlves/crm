package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.AgentContextBuilder;
import com.becommerce.crm.automation.ai.application.agent.AgentContextRenderer;
import com.becommerce.crm.automation.ai.application.agent.context.AgentContext;
import com.becommerce.crm.automation.ai.application.agent.context.AgentRuntimeInput;
import com.becommerce.crm.automation.ai.application.agent.context.ConversationHistory;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolbox;
import com.becommerce.crm.automation.ai.application.port.output.AgentAutoReplyRepository;
import com.becommerce.crm.automation.ai.application.port.output.AgentConfigRepository;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.service.AiChatFailover;
import com.becommerce.crm.communication.omnichannel.application.event.WhatsAppSendEvent;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppEventPublisher;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import com.becommerce.crm.automation.ai.domain.VoiceReplyMode;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageDirection;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Auto-resposta autônoma a mensagens de WhatsApp (Sprint 1 da portabilidade
 * Q7 → CRM). A partir do Sprint 23 o processamento é ASSÍNCRONO (filas
 * RabbitMQ): este component é o <b>consumer lógico da fila {@code crm.whatsapp.auto-ai}</b>,
 * executado fora do request HTTP do webhook. Gera a resposta (IA), persiste o
 * OUTBOUND como PENDING e publica {@link WhatsAppSendEvent} para a fila de
 * sender — o ENVIO em si (provider/Evolution) ocorre no consumer de sender.
 *
 * <p>Regras (safe defaults):
 * <ul>
 *   <li>sem {@link AgentConfig} → nenhuma resposta;</li>
 *   <li>{@code !aiEnabled || !allowAutoReply || sem prompt} → nenhuma resposta;</li>
 *   <li>já respondida para a mensagem entrante (reserva idempotente) → reenvio,
 *       nunca resposta duplicada;</li>
 *   <li>cooldown ({@code cooldownMinutes}) entre respostas na mesma conversa;</li>
 *   <li>a resposta é truncada em {@code maxChars}.</li>
 * </ul>
 *
 * <p>Sprint 2 (IA autônoma robusta):
 * <ul>
 *   <li>params de geração ({@code model}, {@code temperature}, {@code maxTokens})
 *       vêm do {@link AgentConfig} (nulos → default do provider);</li>
 *   <li>geração via {@link AiChatFailover}: timeout controlado, erros classificados,
 *       failover limitado (primário → fallback, nunca infinito);</li>
 *   <li>contexto real da conversa (Conversation/Message, INBOUND→user,
 *       OUTBOUND→assistant) com janela limitada E consistente com o orçamento de
 *       tokens configurado (prioridade: system → mais recentes → mensagem atual).</li>
 * </ul>
 *
 * <p>O pipeline é: reserva → IA (via {@link AiChatFailover}, sem hardcode de
 * prompt) → persiste OUTBOUND pendente → publica {@link WhatsAppSendEvent}
 * (sender consumer envia via {@code WhatsAppProvider}).</p>
 */
@Service
public class WhatsAppInboundAutoReplyProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppInboundAutoReplyProcessor.class);

    private static final int HISTORY_LIMIT = 20;
    private static final int DEFAULT_MAX_TOKENS = 600;
    /**
     * Sem tokenizer (o projeto não possui utilitário de contagem; decisão do
     * Sprint 2), estima-se grosseiramente 1 token ≈ 4 caracteres. O histórico é
     * limitado a ~{@code HISTORY_TOKEN_MULTIPLIER} × o orçamento de saída
     * ({@code maxTokens}), priorizando as mensagens MAIS RECENTES.
     */
    private static final int CHARS_PER_TOKEN = 4;
    private static final int HISTORY_TOKEN_MULTIPLIER = 2;

    private final AgentConfigRepository agentConfigRepository;
    private final AgentAutoReplyRepository autoReplyRepository;
    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelChannelRepository channelRepository;
    private final OmnichannelMessageRepository messageRepository;
    private final AiChatFailover aiChatFailover;
    private final OmnichannelMessagePersister messagePersister;
    private final WhatsAppEventPublisher eventPublisher;
    /**
     * Janela de agrupamento: espera o paciente terminar de digitar. Se chegar
     * outra mensagem nesse intervalo, esta é descartada e a mais recente
     * responde a todas (as anteriores já estão no histórico).
     */
    private final long replyDebounceMillis;
    /** Áudio/foto/PDF → texto. Nulo em testes antigos (mídia fica como placeholder). */
    private final WhatsAppMediaInterpreter mediaInterpreter;
    /** Ponto único de montagem do contexto (configuração + CRM + memória + ferramentas). */
    private final AgentContextBuilder contextBuilder;

    /** Rodadas máximas de ferramenta por resposta (evita laço infinito do modelo). */
    private static final int MAX_TOOL_ROUNDS = 4;

    public WhatsAppInboundAutoReplyProcessor(AgentConfigRepository agentConfigRepository,
                                             AgentAutoReplyRepository autoReplyRepository,
                                             OmnichannelConversationRepository conversationRepository,
                                             OmnichannelChannelRepository channelRepository,
                                             OmnichannelMessageRepository messageRepository,
                                             AiChatFailover aiChatFailover,
                                             OmnichannelMessagePersister messagePersister,
                                             WhatsAppEventPublisher eventPublisher) {
        this(agentConfigRepository, autoReplyRepository, conversationRepository, channelRepository,
                messageRepository, aiChatFailover, messagePersister, eventPublisher, 0L);
    }

    public WhatsAppInboundAutoReplyProcessor(AgentConfigRepository agentConfigRepository,
                                             AgentAutoReplyRepository autoReplyRepository,
                                             OmnichannelConversationRepository conversationRepository,
                                             OmnichannelChannelRepository channelRepository,
                                             OmnichannelMessageRepository messageRepository,
                                             AiChatFailover aiChatFailover,
                                             OmnichannelMessagePersister messagePersister,
                                             WhatsAppEventPublisher eventPublisher,
                                             long replyDebounceSeconds) {
        this(agentConfigRepository, autoReplyRepository, conversationRepository, channelRepository,
                messageRepository, aiChatFailover, messagePersister, eventPublisher, replyDebounceSeconds,
                null, (WhatsAppSchedulingTools) null);
    }

    /**
     * Construtor legado (testes): contexto sem fontes do CRM e, se informada,
     * apenas a agenda como ferramenta.
     */
    public WhatsAppInboundAutoReplyProcessor(AgentConfigRepository agentConfigRepository,
                                             AgentAutoReplyRepository autoReplyRepository,
                                             OmnichannelConversationRepository conversationRepository,
                                             OmnichannelChannelRepository channelRepository,
                                             OmnichannelMessageRepository messageRepository,
                                             AiChatFailover aiChatFailover,
                                             OmnichannelMessagePersister messagePersister,
                                             WhatsAppEventPublisher eventPublisher,
                                             long replyDebounceSeconds,
                                             WhatsAppMediaInterpreter mediaInterpreter,
                                             WhatsAppSchedulingTools schedulingTools) {
        this(agentConfigRepository, autoReplyRepository, conversationRepository, channelRepository,
                messageRepository, aiChatFailover, messagePersister, eventPublisher, replyDebounceSeconds,
                mediaInterpreter, AgentContextBuilder.withoutCrmContext(new AgentToolbox(schedulingTools == null
                        ? List.of() : List.of(new SchedulingToolProvider(schedulingTools)))));
    }

    @Autowired
    public WhatsAppInboundAutoReplyProcessor(AgentConfigRepository agentConfigRepository,
                                             AgentAutoReplyRepository autoReplyRepository,
                                             OmnichannelConversationRepository conversationRepository,
                                             OmnichannelChannelRepository channelRepository,
                                             OmnichannelMessageRepository messageRepository,
                                             AiChatFailover aiChatFailover,
                                             OmnichannelMessagePersister messagePersister,
                                             WhatsAppEventPublisher eventPublisher,
                                             @Value("${omnichannel.whatsapp.reply-debounce-seconds:8}")
                                             long replyDebounceSeconds,
                                             WhatsAppMediaInterpreter mediaInterpreter,
                                             AgentContextBuilder contextBuilder) {
        this.replyDebounceMillis = Math.max(0L, replyDebounceSeconds) * 1000L;
        this.mediaInterpreter = mediaInterpreter;
        this.contextBuilder = contextBuilder;
        this.agentConfigRepository = agentConfigRepository;
        this.autoReplyRepository = autoReplyRepository;
        this.conversationRepository = conversationRepository;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.aiChatFailover = aiChatFailover;
        this.messagePersister = messagePersister;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Processa uma mensagem entrante e, se o agente estiver habilitado, produz a
     * resposta autônoma (persistida como PENDING e enfileirada para envio).
     * Sempre informacional: falhas do agente/IA são registradas e não propagadas
     * (o webhook já confirmou o recebimento).
     */
    public void processInbound(UUID companyId, UUID conversationId, UUID inboundMessageId,
                               String from, String body) {
        processInbound(companyId, conversationId, inboundMessageId, from, body, null);
    }

    public void processInbound(UUID companyId, UUID conversationId, UUID inboundMessageId,
                               String from, String body, String senderName) {
        try {
            TenantContext.setCompanyId(companyId);

            AgentConfig agentConfig = agentConfigRepository.findByCompanyId(companyId).orElse(null);
            if (agentConfig == null) {
                log.debug("Sem AgentConfig (company={}); nenhuma auto-resposta", companyId);
                return;
            }
            if (!agentConfig.canAutoReply() || !agentConfig.hasUsablePrompt()) {
                log.debug("Agente desabilitado (company={}); nenhuma auto-resposta", companyId);
                return;
            }

            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null || !conversation.getCompanyId().equals(companyId)) {
                log.warn("Conversa não encontrada (company={}, conversation={})", companyId, conversationId);
                return;
            }
            if (conversation.isInHumanMode()) {
                log.debug("Conversa em modo humano (company={}, conversation={}); IA autônoma suspensa",
                        companyId, conversationId);
                return;
            }
            Channel channel = channelRepository.findById(conversation.getChannelId()).orElse(null);
            if (channel == null) {
                log.warn("Canal não encontrado (company={}, channel={})", companyId, conversation.getChannelId());
                return;
            }

            if (isWithinCooldown(agentConfig, companyId, conversationId)) {
                log.debug("Auto-resposta em cooldown (company={}, conversation={})", companyId, conversationId);
                return;
            }

            // Mídia é interpretada ANTES do agrupamento: mesmo um áudio "engolido" por
            // uma mensagem seguinte entra transcrito no histórico.
            Message inboundMessage = inboundMessageId == null ? null
                    : messageRepository.findById(inboundMessageId).orElse(null);
            boolean inboundIsAudio = inboundMessage != null && inboundMessage.getType() == MessageType.AUDIO;
            String currentText = body;
            if (mediaInterpreter != null && inboundMessage != null && inboundMessage.getType() != null
                    && inboundMessage.getType() != MessageType.TEXT) {
                currentText = mediaInterpreter.interpret(channel, inboundMessage);
                TenantContext.setCompanyId(companyId);
            }

            if (supersededByNewerInbound(conversationId, inboundMessageId)) {
                log.info("Auto-resposta agrupada: mensagem mais nova na conversa (company={}, conversation={}, inbound={})",
                        companyId, conversationId, inboundMessageId);
                return;
            }

            // Reserva idempotente: no máximo 1 resposta por mensagem entrante.
            if (!autoReplyRepository.reserve(companyId, conversationId, inboundMessageId)) {
                log.debug("Mensagem já respondida (company={}, inbound={}); ignorando", companyId, inboundMessageId);
                return;
            }

            AgentRuntimeInput input = new AgentRuntimeInput(companyId, conversationId, conversation.getContactId(),
                    conversation.getExternalPhone(), senderName, inboundMessageId, currentText,
                    loadHistory(agentConfig, conversationId, inboundMessageId));
            String reply = generateReply(agentConfig, input);
            if (reply == null) {
                return;
            }
            String capped = capLength(reply, agentConfig.getMaxChars());

            Message outbound = Message.createOutbound(companyId, conversationId, channel.getId(),
                    channel.getExternalId(), conversation.getExternalPhone(), capped, UUID.randomUUID());
            Message persisted = messagePersister.persistPending(outbound);

            boolean voice = shouldReplyWithVoice(agentConfig.getVoiceReplyMode(), inboundIsAudio);
            try {
                eventPublisher.publishSend(voice
                        ? WhatsAppSendEvent.voice(companyId, conversationId, persisted.getId(), channel.getId(),
                                conversation.getExternalPhone(), capped)
                        : WhatsAppSendEvent.of(companyId, conversationId, persisted.getId(), channel.getId(),
                                conversation.getExternalPhone(), capped));
            } catch (Exception e) {
                messagePersister.markFailed(persisted.getId(), conversationId, e.getMessage());
                log.warn("Falha ao enfileirar envio de auto-resposta company={} conversation={}: {}",
                        companyId, conversationId, e.getMessage());
            }
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Espera a janela de agrupamento e verifica se o paciente mandou outra
     * mensagem depois desta. Sem janela configurada, nunca agrupa.
     */
    private boolean supersededByNewerInbound(UUID conversationId, UUID inboundMessageId) {
        if (replyDebounceMillis <= 0 || inboundMessageId == null) {
            return false;
        }
        try {
            Thread.sleep(replyDebounceMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return messageRepository.findById(inboundMessageId)
                .map(inbound -> messageRepository.existsInboundAfter(conversationId, inbound.getCreatedAt()))
                .orElse(false);
    }

    static boolean shouldReplyWithVoice(VoiceReplyMode mode, boolean inboundIsAudio) {
        return mode == VoiceReplyMode.ALWAYS || (mode == VoiceReplyMode.MIRROR && inboundIsAudio);
    }

    private String generateReply(AgentConfig agentConfig, AgentRuntimeInput input) {
        UUID conversationId = input.conversationId();
        long generationStart = System.nanoTime();
        try {
            // Contexto montado em UM lugar (AgentContextBuilder) e renderizado em UM lugar.
            AgentContext context = contextBuilder.build(agentConfig, input);
            List<AiProvider.ChatMessage> messages = new ArrayList<>(AgentContextRenderer.render(context));
            List<AiProvider.ToolDefinition> tools = context.tools().definitions();
            AgentToolSession toolSession = contextBuilder.toolSession(agentConfig, input, context.patient());

            AiProvider.GenerationParams params = new AiProvider.GenerationParams(
                    agentConfig.getModel(), agentConfig.getTemperature(), agentConfig.getMaxTokens(), null);
            AiProvider.ChatResult result = null;
            for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
                // Na última rodada, sem ferramentas: o modelo é obrigado a responder em texto.
                List<AiProvider.ToolDefinition> roundTools = round < MAX_TOOL_ROUNDS ? tools : List.of();
                result = aiChatFailover.chat(new AiProvider.ChatRequest(
                        agentConfig.getCompanyId(), null, messages, roundTools).withParams(params));
                if (result == null || !result.hasToolCalls()) {
                    break;
                }
                messages.add(new AiProvider.ChatMessage("assistant", result.content(), result.toolCalls(), null));
                for (AiProvider.ToolCall call : result.toolCalls()) {
                    String output = context.tools().execute(toolSession, call);
                    TenantContext.setCompanyId(agentConfig.getCompanyId());
                    messages.add(new AiProvider.ChatMessage("tool", output, null, call.id()));
                }
            }

            String reply = result != null ? result.content() : null;
            if (reply == null || reply.isBlank()) {
                log.warn("IA retornou resposta vazia (company={}, elapsedMs={})",
                        agentConfig.getCompanyId(), elapsedMillis(generationStart));
                return null;
            }
            log.info("Auto-resposta gerada (company={}, conversation={}, model={}, elapsedMs={})",
                    agentConfig.getCompanyId(), conversationId,
                    params.model() != null ? params.model() : "default", elapsedMillis(generationStart));
            return reply.trim();
        } catch (AiProviderException e) {
            log.error("Falha na geração da auto-resposta (company={}, conversation={}, recuperável={}, elapsedMs={}): {}",
                    agentConfig.getCompanyId(), conversationId, e.isRecoverable(),
                    elapsedMillis(generationStart), safeMessage(e));
            return null;
        }
    }

    /**
     * Histórico recente da conversa REAL de WhatsApp (Conversation/Message — NUNCA
     * AiConversation/AiMessage): INBOUND→user / OUTBOUND→assistant; janela de 20
     * mensagens com poda consistente com o orçamento de tokens ({@code maxTokens}),
     * priorizando as mais recentes. A mensagem em processamento é excluída — ela
     * entra uma única vez como mensagem atual. Histórico NÃO é memória.
     */
    private ConversationHistory loadHistory(AgentConfig agentConfig, UUID conversationId, UUID inboundMessageId) {
        int outputBudget = agentConfig.getMaxTokens() != null && agentConfig.getMaxTokens() > 0
                ? agentConfig.getMaxTokens() : DEFAULT_MAX_TOKENS;
        int historyCharBudget = outputBudget * HISTORY_TOKEN_MULTIPLIER * CHARS_PER_TOKEN;

        List<ConversationHistory.Entry> kept = new ArrayList<>();
        int usedChars = 0;
        List<Message> content = messageRepository.findRecentByConversation(conversationId, HISTORY_LIMIT);
        // Histórico vem em ordem cronológica; percorre dos mais recentes aos mais antigos.
        for (int i = content.size() - 1; i >= 0; i--) {
            Message m = content.get(i);
            if (m.getBody() == null || m.getBody().isBlank()) {
                continue;
            }
            if (inboundMessageId != null && inboundMessageId.equals(m.getId())) {
                continue;
            }
            String role = m.getDirection() == MessageDirection.INBOUND ? "user" : "assistant";
            int estimated = m.getBody().length() + role.length();
            // Sempre mantém pelo menos a mensagem mais recente; depois obedece ao orçamento.
            if (usedChars + estimated > historyCharBudget && !kept.isEmpty()) {
                break;
            }
            kept.add(0, new ConversationHistory.Entry(role, m.getBody()));
            usedChars += estimated;
        }
        return new ConversationHistory(kept);
    }

    private boolean isWithinCooldown(AgentConfig agentConfig, UUID companyId, UUID conversationId) {
        if (agentConfig.getCooldownMinutes() <= 0) {
            return false;
        }
        return autoReplyRepository.lastAutoReplyAt(companyId, conversationId)
                .map(last -> last.plusMinutes(agentConfig.getCooldownMinutes()).isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    private String capLength(String value, int maxChars) {
        return value.length() <= maxChars ? value : value.substring(0, maxChars);
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return msg == null ? t.getClass().getSimpleName() : msg;
    }
}