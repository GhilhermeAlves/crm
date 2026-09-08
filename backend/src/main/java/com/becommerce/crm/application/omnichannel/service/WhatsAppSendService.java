package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.followup.service.FollowUpSendOutcomeHandler;
import com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sender lógico da fila {@code crm.whatsapp.sender} (Sprint 23). Executa o
 * envio EFETIVO via {@link WhatsAppProvider} (UAZAPI continua sendo detalhe de
 * infraestrutura atrás da porta — nenhuma chamada HTTP de provider aqui).
 *
 * <p>Idempotência: a mensagem OUTBOUND é persistida como PENDING pelo produtor
 * (IA ou follow-up); o consumer só envia se ainda estiver PENDING — redelivery
 * após SENT/FAILED é ignorado (nunca dois envios para a mesma mensagem).
 *
 * <p>Tratamento de falha:
 * <ul>
 *   <li>{@link OmnichannelProviderException} (falha SEMÂNTICA do provider) →
 *       marca FAILED local e, para follow-up, aplica retry com backoff/FAILED
 *       terminal via {@link FollowUpSendOutcomeHandler} (idempotente no banco);</li>
 *   <li>runtime inesperado (ex.: banco indisponível) → propaga para o retry do
 *       RabbitMQ e, esgotado, para a DLQ {@code crm.whatsapp.sender.dlq}.</li>
 * </ul>
 */
@Service
public class WhatsAppSendService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSendService.class);

    private final OmnichannelMessageRepository messageRepository;
    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelChannelRepository channelRepository;
    private final WhatsAppProvider whatsAppProvider;
    private final OmnichannelMessagePersister messagePersister;
    private final FollowUpSendOutcomeHandler followUpOutcome;

    public WhatsAppSendService(OmnichannelMessageRepository messageRepository,
                               OmnichannelConversationRepository conversationRepository,
                               OmnichannelChannelRepository channelRepository,
                               WhatsAppProvider whatsAppProvider,
                               OmnichannelMessagePersister messagePersister,
                               FollowUpSendOutcomeHandler followUpOutcome) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.channelRepository = channelRepository;
        this.whatsAppProvider = whatsAppProvider;
        this.messagePersister = messagePersister;
        this.followUpOutcome = followUpOutcome;
    }

    public void send(WhatsAppSendEvent event) {
        if (event == null || event.companyId() == null || event.outboundMessageId() == null
                || event.conversationId() == null) {
            log.warn("[WHATSAPP][SENDER] evento inválido; rejeitado com segurança");
            return;
        }
        UUID companyId = event.companyId();
        TenantContext.setCompanyId(companyId);
        try {
            Message message = messageRepository.findById(event.outboundMessageId()).orElse(null);
            if (message == null || !message.getCompanyId().equals(companyId)) {
                log.warn("[WHATSAPP][SENDER] eventId={} companyId={} outboundMessageId={} não encontrada; descartando",
                        event.eventId(), companyId, event.outboundMessageId());
                return;
            }
            if (message.getStatus() != MessageStatus.PENDING) {
                log.info("[WHATSAPP][SENDER] eventId={} companyId={} messageId={} já {}; duplicata ignorada",
                        event.eventId(), companyId, message.getId(), message.getStatus());
                return;
            }
            Conversation conversation = conversationRepository.findById(event.conversationId()).orElse(null);
            if (conversation == null || !conversation.getCompanyId().equals(companyId)) {
                log.warn("[WHATSAPP][SENDER] conversa inexistente (company={}, conversation={})",
                        companyId, event.conversationId());
                failPermanently(message, event, "Conversa não encontrada");
                return;
            }
            Channel channel = channelRepository.findById(event.channelId()).orElse(null);
            if (channel == null || !channel.getCompanyId().equals(companyId)) {
                log.warn("[WHATSAPP][SENDER] canal inexistente (company={}, channel={})",
                        companyId, event.channelId());
                failPermanently(message, event, "Canal não encontrado");
                return;
            }

            try {
                WhatsAppProvider.SendResult result = whatsAppProvider.send(
                        new WhatsAppProvider.SendRequest(companyId, channel.getId(),
                                channel.getExternalId(), conversation.getExternalPhone(),
                                message.getBody(), channel.getSecretsRef()));
                messagePersister.markSent(message.getId(), event.conversationId(), result.externalMessageId());
                if (event.followUpId() != null) {
                    followUpOutcome.markSent(companyId, event.followUpId(), result.externalMessageId());
                }
                log.info("[WHATSAPP][SENDER] eventId={} companyId={} conversationId={} messageId={} provider={} sent",
                        event.eventId(), companyId, event.conversationId(), message.getId(),
                        whatsAppProvider.providerName());
            } catch (OmnichannelProviderException e) {
                String error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                messagePersister.markFailed(message.getId(), event.conversationId(), error);
                if (event.followUpId() != null) {
                    followUpOutcome.onSendFailed(companyId, event.followUpId(), error);
                }
                log.warn("[WHATSAPP][SENDER] eventId={} companyId={} conversationId={} falha provider: {}",
                        event.eventId(), companyId, event.conversationId(), error);
            }
        } finally {
            TenantContext.clear();
        }
    }

    /** Dados de referência ausentes/inconsistentes: falha permanente, sem retry. */
    private void failPermanently(Message message, WhatsAppSendEvent event, String error) {
        messagePersister.markFailed(message.getId(), event.conversationId(), error);
        if (event.followUpId() != null) {
            followUpOutcome.onSendFailed(event.companyId(), event.followUpId(), error);
        }
    }
}