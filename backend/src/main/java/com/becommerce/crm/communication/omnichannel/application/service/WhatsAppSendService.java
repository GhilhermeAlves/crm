package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.sales.followup.application.service.FollowUpSendOutcomeHandler;
import com.becommerce.crm.automation.ai.application.port.output.AiMediaProvider;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import com.becommerce.crm.communication.omnichannel.application.event.WhatsAppSendEvent;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Sender lógico da fila {@code crm.whatsapp.sender} (Sprint 23). Executa o
 * envio EFETIVO via {@link WhatsAppProvider} (Evolution continua sendo detalhe de
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
    /** Gera a fala das respostas em voz; nulo (testes) envia sempre texto. */
    private final AiMediaProvider mediaProvider;

    public WhatsAppSendService(OmnichannelMessageRepository messageRepository,
                               OmnichannelConversationRepository conversationRepository,
                               OmnichannelChannelRepository channelRepository,
                               WhatsAppProvider whatsAppProvider,
                               OmnichannelMessagePersister messagePersister,
                               FollowUpSendOutcomeHandler followUpOutcome) {
        this(messageRepository, conversationRepository, channelRepository, whatsAppProvider, messagePersister,
                followUpOutcome, null);
    }

    @Autowired
    public WhatsAppSendService(OmnichannelMessageRepository messageRepository,
                               OmnichannelConversationRepository conversationRepository,
                               OmnichannelChannelRepository channelRepository,
                               WhatsAppProvider whatsAppProvider,
                               OmnichannelMessagePersister messagePersister,
                               FollowUpSendOutcomeHandler followUpOutcome,
                               AiMediaProvider mediaProvider) {
        this.mediaProvider = mediaProvider;
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
                WhatsAppProvider.SendRequest request = new WhatsAppProvider.SendRequest(companyId, channel.getId(),
                        channel.getExternalId(), conversation.getExternalPhone(),
                        message.getBody(), channel.getSecretsRef());
                WhatsAppProvider.SendResult result = event.voice() && mediaProvider != null
                        ? sendAsVoice(request, message, channel)
                        : whatsAppProvider.send(request);
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

    /**
     * Resposta em voz: gera a fala e envia como nota de voz. Datas, horários e
     * valores também seguem por escrito (ninguém copia horário de áudio). Se a voz
     * falhar, envia o texto — o paciente nunca fica sem resposta.
     */
    private WhatsAppProvider.SendResult sendAsVoice(WhatsAppProvider.SendRequest request, Message message,
                                                    Channel channel) {
        WhatsAppProvider.SendResult voiceResult;
        try {
            byte[] audio = mediaProvider.speech(request.body());
            voiceResult = whatsAppProvider.sendVoice(request, audio);
        } catch (AiProviderException | OmnichannelProviderException e) {
            log.warn("[WHATSAPP][SENDER] voz indisponível (company={}, message={}): {}; enviando texto",
                    request.companyId(), message.getId(), e.getMessage());
            return whatsAppProvider.send(request);
        }
        String keyInfo = keyInfo(request.body());
        if (keyInfo != null) {
            try {
                // Gravada como OUTBOUND própria: aparece na Inbox e o eco não é tomado por resposta manual.
                Message written = Message.createOutbound(request.companyId(), message.getConversationId(),
                        channel.getId(), channel.getExternalId(), request.to(), keyInfo, UUID.randomUUID());
                Message saved = messageRepository.save(written);
                WhatsAppProvider.SendResult textResult = whatsAppProvider.send(new WhatsAppProvider.SendRequest(
                        request.companyId(), request.channelId(), request.phoneNumberId(), request.to(), keyInfo,
                        request.secretsRef()));
                messagePersister.markSent(saved.getId(), message.getConversationId(), textResult.externalMessageId());
            } catch (RuntimeException e) {
                log.warn("[WHATSAPP][SENDER] falha no complemento escrito da voz (message={}): {}",
                        message.getId(), e.getMessage());
            }
        }
        return voiceResult;
    }

    /** Frases com números (datas, horários, valores) para mandar também por escrito; nulo se não houver. */
    static String keyInfo(String text) {
        if (text == null || !text.matches("(?s).*\\d.*")) {
            return null;
        }
        List<String> parts = new java.util.ArrayList<>();
        for (String sentence : text.split("(?<=[.!?\\n])\\s+")) {
            if (sentence.matches("(?s).*\\d.*")) {
                parts.add(sentence.trim());
            }
        }
        return parts.isEmpty() ? null : "📌 " + String.join(" ", parts);
    }

    /** Dados de referência ausentes/inconsistentes: falha permanente, sem retry. */
    private void failPermanently(Message message, WhatsAppSendEvent event, String error) {
        messagePersister.markFailed(message.getId(), event.conversationId(), error);
        if (event.followUpId() != null) {
            followUpOutcome.onSendFailed(event.companyId(), event.followUpId(), error);
        }
    }
}