package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.event.FollowUpExecutionEvent;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppEventPublisher;
import com.becommerce.crm.application.omnichannel.service.OmnichannelMessagePersister;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.FollowUpCancellationReason;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Executor lógico da fila {@code crm.followup.executor} (Sprint 23).
 *
 * <p>O scheduler já fez o CLAIM atômico (PENDING → PROCESSING); este consumer
 * valida o estado atual e PREPARA o envio de forma assíncrona: valida conversa,
 * Human Takeover e staleness (cliente respondeu), persiste a mensagem OUTBOUND
 * como PENDING e publica {@code WhatsAppSendEvent} (seguimento opcional) — o
 * envio efetivo ocorre no consumer de sender.
 *
 * <p>Idempotência: o OUTBOUND é ancorado por {@code clientMessageId = followUpId}
 * (índice único {@code (company_id, client_message_id)}). Em reexecução o
 * executor reutiliza a mensagem existente — PENDING → republica (recuperação de
 * publish perdido); SENT/FAILED → ignora (já tratado pelo sender/desfecho).
 */
@Service
public class FollowUpExecutionService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpExecutionService.class);

    private final FollowUpRepository followUpRepository;
    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelChannelRepository channelRepository;
    private final OmnichannelMessageRepository messageRepository;
    private final OmnichannelMessagePersister messagePersister;
    private final WhatsAppEventPublisher eventPublisher;
    private final TenantAuditRecorder auditor;

    public FollowUpExecutionService(FollowUpRepository followUpRepository,
                                    OmnichannelConversationRepository conversationRepository,
                                    OmnichannelChannelRepository channelRepository,
                                    OmnichannelMessageRepository messageRepository,
                                    OmnichannelMessagePersister messagePersister,
                                    WhatsAppEventPublisher eventPublisher,
                                    TenantAuditRecorder auditor) {
        this.followUpRepository = followUpRepository;
        this.conversationRepository = conversationRepository;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.messagePersister = messagePersister;
        this.eventPublisher = eventPublisher;
        this.auditor = auditor;
    }

    public void execute(FollowUpExecutionEvent event) {
        if (event == null || event.companyId() == null || event.followUpId() == null) {
            log.warn("[FOLLOWUP][EXECUTOR] evento inválido; rejeitado com segurança");
            return;
        }
        UUID companyId = event.companyId();
        TenantContext.setCompanyId(companyId);
        try {
            FollowUp followUp = followUpRepository.findById(event.followUpId()).orElse(null);
            if (followUp == null || !followUp.getCompanyId().equals(companyId)) {
                log.warn("[FOLLOWUP][EXECUTOR] eventId={} companyId={} followUpId={} não encontrado; descartando",
                        event.eventId(), companyId, event.followUpId());
                return;
            }
            if (!followUp.isProcessing()) {
                log.info("[FOLLOWUP][EXECUTOR] followUpId={} já {}; duplicata ignorada",
                        event.followUpId(), followUp.getStatus());
                return;
            }

            Conversation conversation = conversationRepository.findById(followUp.getConversationId()).orElse(null);
            if (conversation == null || !conversation.getCompanyId().equals(companyId)) {
                followUpRepository.markFailedTerminal(companyId, followUp.getId(), "Conversa não encontrada",
                        LocalDateTime.now());
                return;
            }
            if (conversation.isInHumanMode()) {
                cancelByRule(companyId, followUp.getId(), FollowUpCancellationReason.HUMAN_MODE);
                return;
            }
            if (messageRepository.existsInboundAfter(followUp.getConversationId(), followUp.getCreatedAt())) {
                cancelByRule(companyId, followUp.getId(), FollowUpCancellationReason.SUPERSEDED_BY_NEW_MESSAGE);
                return;
            }

            switch (followUp.getActionType()) {
                case SEND_MESSAGE -> prepareSend(companyId, followUp, conversation);
                default -> followUpRepository.markFailedTerminal(companyId, followUp.getId(),
                        "Ação de follow-up não suportada: " + followUp.getActionType(), LocalDateTime.now());
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void prepareSend(UUID companyId, FollowUp followUp, Conversation conversation) {
        UUID clientMessageId = followUp.getId();

        Optional<Message> existing = messageRepository.findByClientMessageId(clientMessageId);
        if (existing.isPresent() && existing.get().getStatus() != MessageStatus.PENDING) {
            log.info("[FOLLOWUP][EXECUTOR] followUpId={} mensagem já {}; desfecho já tratado",
                    followUp.getId(), existing.get().getStatus());
            return;
        }

        Channel channel = channelRepository.findById(conversation.getChannelId())
                .orElseThrow(() -> new OmnichannelNotFoundException(conversation.getChannelId(), "Canal"));
        if (existing.isPresent()) {
            republishSend(companyId, followUp, conversation, channel, existing.get());
            return;
        }

        Message message = Message.createOutbound(companyId, conversation.getId(), channel.getId(),
                channel.getExternalId(), conversation.getExternalPhone(), followUp.getActionContent(), clientMessageId);
        Message persisted;
        try {
            persisted = messagePersister.persistPending(message);
        } catch (DataIntegrityViolationException e) {
            // Corrida: outro executor persistiu a mesma mensagem (clientMessageId único).
            log.debug("[FOLLOWUP][EXECUTOR] corrida de persistência (followUpId={}); reutilizando",
                    followUp.getId());
            Message winner = messageRepository.findByClientMessageId(clientMessageId).orElse(null);
            if (winner == null) {
                throw e;
            }
            persisted = winner;
        }
        publishSend(companyId, followUp, conversation, channel, persisted);
    }

    private void republishSend(UUID companyId, FollowUp followUp, Conversation conversation,
                               Channel channel, Message message) {
        log.info("[FOLLOWUP][EXECUTOR] followUpId={} republicando envio (mensagem PENDING)",
                followUp.getId());
        publishSend(companyId, followUp, conversation, channel, message);
    }

    private void publishSend(UUID companyId, FollowUp followUp, Conversation conversation,
                             Channel channel, Message message) {
        try {
            eventPublisher.publishSend(com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent
                    .ofFollowUp(companyId, conversation.getId(), message.getId(), channel.getId(),
                            conversation.getExternalPhone(), followUp.getActionContent(), followUp.getId()));
        } catch (Exception e) {
            messagePersister.markFailed(message.getId(), conversation.getId(), e.getMessage());
            log.warn("[FOLLOWUP][EXECUTOR] falha ao enfileirar envio followUpId={}: {}",
                    followUp.getId(), e.getMessage());
            followUpRepository.scheduleRetry(companyId, followUp.getId(),
                    LocalDateTime.now().plus(FollowUpSendOutcomeHandler.backoff(1)), e.getMessage(),
                    LocalDateTime.now());
        }
    }

    private void cancelByRule(UUID companyId, UUID followUpId, FollowUpCancellationReason reason) {
        if (followUpRepository.cancelProcessingByRule(companyId, followUpId, reason, LocalDateTime.now())) {
            audit(companyId, followUpId, AuditAction.UPDATE, "Follow-up cancelado por regra (" + reason + ")");
        }
    }

    private void audit(UUID companyId, UUID followUpId, AuditAction action, String description) {
        try {
            auditor.record(companyId, action, AuditModule.FOLLOWUPS, "FollowUp",
                    followUpId.toString(), description, null, null);
        } catch (RuntimeException e) {
            log.warn("Falha ao registrar auditoria de processamento de FollowUp (company={}, followUp={}): {}",
                    companyId, followUpId, e.getMessage());
        }
    }
}