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
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Auto-resposta autônoma a mensagens de WhatsApp (Sprint 1 da portabilidade
 * Q7 → CRM). Consumido por um listener {@code AFTER_COMMIT} (fora da transação
 * do webhook) — as chamadas de LLM e WhatsApp ocorrem SEM transação de BD aberta.
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
 * prompt) → persiste OUTBOUND pendente → envia via {@link WhatsAppProvider} →
 * marca enviado/falha com {@link OmnichannelMessagePersister} (REQUIRES_NEW).</p>
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
    private final WhatsAppProvider whatsAppProvider;
    private final AiChatFailover aiChatFailover;
    private final OmnichannelMessagePersister messagePersister;

    public WhatsAppInboundAutoReplyProcessor(AgentConfigRepository agentConfigRepository,
                                             AgentAutoReplyRepository autoReplyRepository,
                                             OmnichannelConversationRepository conversationRepository,
                                             OmnichannelChannelRepository channelRepository,
                                             OmnichannelMessageRepository messageRepository,
                                             WhatsAppProvider whatsAppProvider,
                                             AiChatFailover aiChatFailover,
                                             OmnichannelMessagePersister messagePersister) {
        this.agentConfigRepository = agentConfigRepository;
        this.autoReplyRepository = autoReplyRepository;
        this.conversationRepository = conversationRepository;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.whatsAppProvider = whatsAppProvider;
        this.aiChatFailover = aiChatFailover;
        this.messagePersister = messagePersister;
    }

    /**
     * Processa uma mensagem entrante e, se o agente estiver habilitado, produz a
     * resposta autônoma. Sempre informacional: falhas do agente/IA/provedor são
     * registradas e não propagadas (o webhook já confirmou o recebimento).
     */
    public void processInbound(UUID companyId, UUID conversationId, UUID inboundMessageId,
                               String from, String body) {
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
            Channel channel = channelRepository.findById(conversation.getChannelId()).orElse(null);
            if (channel == null) {
                log.warn("Canal não encontrado (company={}, channel={})", companyId, conversation.getChannelId());
                return;
            }

            if (isWithinCooldown(agentConfig, companyId, conversationId)) {
                log.debug("Auto-resposta em cooldown (company={}, conversation={})", companyId, conversationId);
                return;
            }

            // Reserva idempotente: no máximo 1 resposta por mensagem entrante.
            if (!autoReplyRepository.reserve(companyId, conversationId, inboundMessageId)) {
                log.debug("Mensagem já respondida (company={}, inbound={}); ignorando", companyId, inboundMessageId);
                return;
            }

            String reply = generateReply(agentConfig, conversationId, inboundMessageId, body);
            if (reply == null) {
                return;
            }
            String capped = capLength(reply, agentConfig.getMaxChars());

            Message outbound = Message.createOutbound(companyId, conversationId, channel.getId(),
                    channel.getExternalId(), conversation.getExternalPhone(), capped, UUID.randomUUID());
            Message persisted = messagePersister.persistPending(outbound);

            long sendStart = System.nanoTime();
            try {
                WhatsAppProvider.SendResult result = whatsAppProvider.send(
                        new WhatsAppProvider.SendRequest(companyId, channel.getId(),
                                channel.getExternalId(), conversation.getExternalPhone(), capped,
                                channel.getSecretsRef()));
                messagePersister.markSent(persisted.getId(), conversationId, result.externalMessageId());
                log.info("Auto-resposta enviada (company={}, conversation={}, inboundMessageId={}, provider={}, elapsedMs={})",
                        companyId, conversationId, inboundMessageId,
                        whatsAppProvider.providerName(), elapsedMillis(sendStart));
            } catch (OmnichannelProviderException e) {
                // Persistido em REQUIRES_NEW: sobrevive a falhas e não quebra o webhook.
                messagePersister.markFailed(persisted.getId(), conversationId, e.getMessage());
                log.warn("Falha ao enviar auto-resposta company={} conversation={}: {}",
                        companyId, conversationId, e.getMessage());
            }
        } finally {
            TenantContext.clear();
        }
    }

    private String generateReply(AgentConfig agentConfig, UUID conversationId,
                                 UUID inboundMessageId, String body) {
        long generationStart = System.nanoTime();
        try {
            List<AiProvider.ChatMessage> messages =
                    buildContext(agentConfig, conversationId, inboundMessageId, body);

            AiProvider.GenerationParams params = new AiProvider.GenerationParams(
                    agentConfig.getModel(), agentConfig.getTemperature(), agentConfig.getMaxTokens(), null);
            AiProvider.ChatResult result = aiChatFailover.chat(new AiProvider.ChatRequest(
                    agentConfig.getCompanyId(), null, messages).withParams(params));

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
     * Monta o contexto da conversa REAL de WhatsApp (Conversation/Message — NUNCA
     * AiConversation/AiMessage): system prompt exclusivamente do AgentConfig;
     * histórico INBOUND→user / OUTBOUND→assistant; janela de 20 mensagens com
     * poda consistente com o orçamento de tokens ({@code maxTokens}); mensagem
     * atual sempre incluída (uma única vez — o inbound em curso é excluído do
     * histórico e reaparece como a última mensagem {@code user}). Prioridade:
     * system → mais recentes → atual.
     */
    private List<AiProvider.ChatMessage> buildContext(AgentConfig agentConfig, UUID conversationId,
                                                      UUID inboundMessageId, String body) {
        List<AiProvider.ChatMessage> messages = new ArrayList<>();
        messages.add(new AiProvider.ChatMessage("system", agentConfig.getSystemPrompt()));

        int outputBudget = agentConfig.getMaxTokens() != null && agentConfig.getMaxTokens() > 0
                ? agentConfig.getMaxTokens() : DEFAULT_MAX_TOKENS;
        int historyCharBudget = outputBudget * HISTORY_TOKEN_MULTIPLIER * CHARS_PER_TOKEN;

        PageResponse<Message> history = messageRepository.findByConversation(conversationId, 0, HISTORY_LIMIT);
        List<AiProvider.ChatMessage> kept = new ArrayList<>();
        int usedChars = 0;
        List<Message> content = history.content();
        // Histórico vem em ordem cronológica; percorre dos mais recentes aos mais antigos.
        for (int i = content.size() - 1; i >= 0; i--) {
            Message m = content.get(i);
            if (m.getBody() == null || m.getBody().isBlank()) {
                continue;
            }
            // A mensagem entrante em processamento é a "mensagem atual": entra uma
            // única vez (como última mensagem user), evitando duplicação no prompt.
            if (inboundMessageId != null && inboundMessageId.equals(m.getId())) {
                continue;
            }
            String role = m.getDirection() == MessageDirection.INBOUND ? "user" : "assistant";
            int estimated = m.getBody().length() + role.length();
            // Sempre mantém pelo menos a mensagem mais recente; depois obedece ao orçamento.
            if (usedChars + estimated > historyCharBudget && !kept.isEmpty()) {
                break;
            }
            kept.add(0, new AiProvider.ChatMessage(role, m.getBody()));
            usedChars += estimated;
        }
        messages.addAll(kept);
        messages.add(new AiProvider.ChatMessage("user", body));
        return messages;
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