package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.application.omnichannel.service.OmnichannelMessagePersister;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.FollowUpCancellationReason;
import com.becommerce.crm.domain.followup.exception.FollowUpValidationException;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Executor de FollowUp (Sprint 22), chamado pelo {@code FollowUpScheduler}
 * (thread sem contexto HTTP). Para casa candidato:
 * <ol>
 *   <li><b>Claim atômico</b> — {@code PENDING -> PROCESSING} (ou recuperação de
 *       PROCESSING órfão após crash). Um único worker/instância vence.</li>
 *   <li><b>Segurança/human takeover</b> — conversa em modo HUMAN: NUNCA executa;
 *       cancela por regra {@code HUMAN_MODE}.</li>
 *   <li><b>Staleness</b> — se o cliente enviou nova mensagem INBOUND depois do
 *       agendamento, o follow-up está obsoleto: cancela
 *       {@code SUPERSEDED_BY_NEW_MESSAGE} (determinístico).</li>
 *   <li><b>Ação</b> — {@code SEND_MESSAGE}: persiste a mensagem (REQUIRES_NEW),
 *       envia via WhatsAppProvider e marca SENT; em falha, retry seguro com
 *       backoff ou FAILED terminal (idempotente).</li>
 * </ol>
 */
@Service
public class FollowUpProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpProcessingService.class);

    /** Um PROCESSING mais antigo que isso é tratado como órfão (recuperação de crash). */
    public static final Duration STALE_PROCESSING_CUTOFF = Duration.ofMinutes(15);

    private final FollowUpRepository followUpRepository;
    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelChannelRepository channelRepository;
    private final OmnichannelMessageRepository messageRepository;
    private final WhatsAppProvider whatsAppProvider;
    private final OmnichannelMessagePersister messagePersister;
    private final TenantAuditRecorder auditor;

    public FollowUpProcessingService(FollowUpRepository followUpRepository,
                                     OmnichannelConversationRepository conversationRepository,
                                     OmnichannelChannelRepository channelRepository,
                                     OmnichannelMessageRepository messageRepository,
                                     WhatsAppProvider whatsAppProvider,
                                     OmnichannelMessagePersister messagePersister,
                                     TenantAuditRecorder auditor) {
        this.followUpRepository = followUpRepository;
        this.conversationRepository = conversationRepository;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.whatsAppProvider = whatsAppProvider;
        this.messagePersister = messagePersister;
        this.auditor = auditor;
    }

    public void process(UUID companyId, UUID followUpId) {
        LocalDateTime now = LocalDateTime.now();
        TenantContext.setCompanyId(companyId);
        try {
            boolean claimed = followUpRepository.claim(companyId, followUpId, now, now.minus(STALE_PROCESSING_CUTOFF));
            if (!claimed) {
                return;
            }
            FollowUp followUp = followUpRepository.findById(followUpId).orElse(null);
            if (followUp == null) {
                return;
            }
            if (!followUp.getCompanyId().equals(companyId)) {
                return;
            }

            Conversation conversation = conversationRepository.findById(followUp.getConversationId()).orElse(null);
            if (conversation == null || !conversation.getCompanyId().equals(companyId)) {
                followUpRepository.markFailedTerminal(companyId, followUpId, "Conversa não encontrada", now);
                return;
            }
            if (conversation.isInHumanMode()) {
                cancelByRule(companyId, followUpId, FollowUpCancellationReason.HUMAN_MODE, now);
                return;
            }
            if (messageRepository.existsInboundAfter(followUp.getConversationId(), followUp.getCreatedAt())) {
                cancelByRule(companyId, followUpId, FollowUpCancellationReason.SUPERSEDED_BY_NEW_MESSAGE, now);
                return;
            }

            switch (followUp.getActionType()) {
                case SEND_MESSAGE -> executeSendMessage(companyId, followUp, conversation, now);
                default -> throw new FollowUpValidationException(
                        "Ação de follow-up não suportada: " + followUp.getActionType());
            }
        } catch (Exception e) {
            log.error("Falha ao processar follow-up {} (company={}): {}",
                    followUpId, companyId, e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private void executeSendMessage(UUID companyId, FollowUp followUp, Conversation conversation, LocalDateTime now) {
        Channel channel = channelRepository.findById(conversation.getChannelId())
                .orElseThrow(() -> new OmnichannelNotFoundException(conversation.getChannelId(), "Canal"));
        String body = followUp.getActionContent();
        Message message = Message.createOutbound(companyId, conversation.getId(), channel.getId(),
                channel.getExternalId(), conversation.getExternalPhone(), body, UUID.randomUUID());
        Message persisted = messagePersister.persistPending(message);

        try {
            WhatsAppProvider.SendResult result = whatsAppProvider.send(
                    new WhatsAppProvider.SendRequest(companyId, channel.getId(),
                            channel.getExternalId(), conversation.getExternalPhone(), body,
                            channel.getSecretsRef()));
            messagePersister.markSent(persisted.getId(), conversation.getId(), result.externalMessageId());
            followUpRepository.markSent(companyId, followUp.getId(), result.externalMessageId(), LocalDateTime.now());
            audit(companyId, followUp.getId(), AuditAction.UPDATE,
                    "Follow-up executado (mensagem enviada: " + result.externalMessageId() + ")");
        } catch (OmnichannelProviderException e) {
            messagePersister.markFailed(persisted.getId(), conversation.getId(), e.getMessage());
            handleSendFailure(companyId, followUp, e.getMessage());
        }
    }

    private void handleSendFailure(UUID companyId, FollowUp followUp, String error) {
        LocalDateTime now = LocalDateTime.now();
        int attempt = followUp.getAttempts() + 1;
        if (attempt >= FollowUp.MAX_ATTEMPTS) {
            if (followUpRepository.markFailedTerminal(companyId, followUp.getId(), error, now)) {
                audit(companyId, followUp.getId(), AuditAction.UPDATE,
                        "Follow-up falhou definitivamente após " + attempt + " tentativas");
            }
            return;
        }
        if (followUpRepository.scheduleRetry(companyId, followUp.getId(), now.plus(backoff(attempt)), error, now)) {
            audit(companyId, followUp.getId(), AuditAction.UPDATE,
                    "Follow-up reagendado após falha (tentativa " + attempt + ")");
        }
    }

    /** Backoff progressivo: 15min, 1h, 4h. */
    static Duration backoff(int attempt) {
        return switch (attempt) {
            case 1 -> Duration.ofMinutes(15);
            case 2 -> Duration.ofHours(1);
            default -> Duration.ofHours(4);
        };
    }

    private void cancelByRule(UUID companyId, UUID followUpId, FollowUpCancellationReason reason, LocalDateTime now) {
        if (followUpRepository.cancelProcessingByRule(companyId, followUpId, reason, now)) {
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