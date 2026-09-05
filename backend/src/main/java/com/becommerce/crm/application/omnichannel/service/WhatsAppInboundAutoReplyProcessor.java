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
 * <p>O pipeline é: reserva → IA (via {@link AiProvider}, sem hardcode de prompt)
 * → persiste OUTBOUND pendente → envia via {@link WhatsAppProvider} → marca
 * enviado/falha com {@link OmnichannelMessagePersister} (REQUIRES_NEW).</p>
 */
@Service
public class WhatsAppInboundAutoReplyProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppInboundAutoReplyProcessor.class);

    private static final int HISTORY_LIMIT = 20;

    private final AgentConfigRepository agentConfigRepository;
    private final AgentAutoReplyRepository autoReplyRepository;
    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelChannelRepository channelRepository;
    private final OmnichannelMessageRepository messageRepository;
    private final WhatsAppProvider whatsAppProvider;
    private final AiProvider aiProvider;
    private final OmnichannelMessagePersister messagePersister;

    public WhatsAppInboundAutoReplyProcessor(AgentConfigRepository agentConfigRepository,
                                             AgentAutoReplyRepository autoReplyRepository,
                                             OmnichannelConversationRepository conversationRepository,
                                             OmnichannelChannelRepository channelRepository,
                                             OmnichannelMessageRepository messageRepository,
                                             WhatsAppProvider whatsAppProvider,
                                             AiProvider aiProvider,
                                             OmnichannelMessagePersister messagePersister) {
        this.agentConfigRepository = agentConfigRepository;
        this.autoReplyRepository = autoReplyRepository;
        this.conversationRepository = conversationRepository;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.whatsAppProvider = whatsAppProvider;
        this.aiProvider = aiProvider;
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

            String reply = generateReply(agentConfig, conversationId, body);
            if (reply == null) {
                return;
            }
            String capped = capLength(reply, agentConfig.getMaxChars());

            Message outbound = Message.createOutbound(companyId, conversationId, channel.getId(),
                    channel.getExternalId(), conversation.getExternalPhone(), capped, UUID.randomUUID());
            Message persisted = messagePersister.persistPending(outbound);

            try {
                WhatsAppProvider.SendResult result = whatsAppProvider.send(
                        new WhatsAppProvider.SendRequest(companyId, channel.getId(),
                                channel.getExternalId(), conversation.getExternalPhone(), capped,
                                channel.getSecretsRef()));
                messagePersister.markSent(persisted.getId(), conversationId, result.externalMessageId());
                log.info("Auto-resposta enviada (company={}, conversation={}, inboundMessageId={})",
                        companyId, conversationId, inboundMessageId);
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

    private String generateReply(AgentConfig agentConfig, UUID conversationId, String body) {
        try {
            List<AiProvider.ChatMessage> messages = new ArrayList<>();
            messages.add(new AiProvider.ChatMessage("system", agentConfig.getSystemPrompt()));

            PageResponse<Message> history = messageRepository.findByConversation(conversationId, 0, HISTORY_LIMIT);
            for (Message m : history.content()) {
                if (m.getBody() == null || m.getBody().isBlank()) {
                    continue;
                }
                String role = m.getDirection() == MessageDirection.INBOUND ? "user" : "assistant";
                messages.add(new AiProvider.ChatMessage(role, m.getBody()));
            }
            messages.add(new AiProvider.ChatMessage("user", body));

            String reply = aiProvider.chat(new AiProvider.ChatRequest(
                    agentConfig.getCompanyId(), null, messages));
            if (reply == null || reply.isBlank()) {
                log.warn("IA retornou resposta vazia (company={})", agentConfig.getCompanyId());
                return null;
            }
            return reply.trim();
        } catch (AiProviderException e) {
            log.error("Falha na geração da auto-resposta (company={}): {}",
                    agentConfig.getCompanyId(), e.getMessage());
            return null;
        }
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
}