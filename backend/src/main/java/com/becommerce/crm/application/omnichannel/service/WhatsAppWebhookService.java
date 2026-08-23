package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.omnichannel.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelCompanyResolver;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook de WhatsApp (Sprint 16, FASE 6/10/17). Recebe eventos do provider,
 * resolve a empresa pelo canal (SECURITY DEFINER, sem sessão), persiste de
 * forma idempotente e publica o evento de workflow {@code WHATSAPP_MESSAGE_RECEIVED}.
 *
 * <p>Idempotência garantida por constraint única do banco
 * ({@code external_message_id} / {@code client_message_id}) — não por lógica Java.
 */
@Service
public class WhatsAppWebhookService implements WhatsAppWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);

    private final WhatsAppWebhookParser parser;
    private final OmnichannelCompanyResolver companyResolver;
    private final OmnichannelChannelRepository channelRepository;
    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelMessageRepository messageRepository;
    private final ContactRepository contactRepository;
    private final EventPublisher eventPublisher;
    private final String verificationToken;

    public WhatsAppWebhookService(WhatsAppWebhookParser parser,
                                  OmnichannelCompanyResolver companyResolver,
                                  OmnichannelChannelRepository channelRepository,
                                  OmnichannelConversationRepository conversationRepository,
                                  OmnichannelMessageRepository messageRepository,
                                  ContactRepository contactRepository,
                                  EventPublisher eventPublisher,
                                  @Value("${omnichannel.whatsapp.webhook-verify-token:}") String verificationToken) {
        this.parser = parser;
        this.companyResolver = companyResolver;
        this.channelRepository = channelRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.contactRepository = contactRepository;
        this.eventPublisher = eventPublisher;
        this.verificationToken = verificationToken;
    }

    @Override
    public String verify(Map<String, String> params) {
        WhatsAppWebhookParser.Verification v = parser.parseVerification(params);
        if (v.token() == null || v.token().isBlank() || !v.token().equals(verificationToken)) {
            return null;
        }
        return v.challenge();
    }

    @Override
    @Transactional
    public void handleEvent(Map<String, Object> payload) {
        try {
            String channelRef = parser.providerChannelReference(payload);
            if (channelRef == null || channelRef.isBlank()) {
                log.warn("Webhook sem referência de canal; ignorando");
                return;
            }
            UUID companyId = companyResolver.resolveCompanyByChannelReference(channelRef).orElse(null);
            if (companyId == null) {
                log.warn("Webhook para canal desconhecido ({}); ignorando", channelRef);
                return;
            }
            TenantContext.setCompanyId(companyId);

            if (parser.isInboundMessage(payload)) {
                handleInbound(companyId, payload);
            } else if (parser.isStatusUpdate(payload)) {
                handleStatus(companyId, payload);
            } else {
                log.info("Webhook sem conteúdo processável (company={})", companyId);
            }
        } catch (Exception e) {
            // Nunca logar payload/secrets; apenas ids e mensagem de erro.
            log.error("Erro ao processar webhook: {}", e.getMessage());
            throw e;
        } finally {
            TenantContext.clear();
        }
    }

    private void handleInbound(UUID companyId, Map<String, Object> payload) {
        WhatsAppWebhookParser.InboundMessageData data = parser.parseInboundMessage(payload)
                .orElse(null);
        if (data == null) {
            log.warn("Webhook de mensagem sem dados válidos (company={})", companyId);
            return;
        }
        // Idempotência: já registrada?
        if (messageRepository.findByExternalMessageId(data.externalMessageId()).isPresent()) {
            log.info("Mensagem já registrada (externalId={}); ignorando duplicada", data.externalMessageId());
            return;
        }
        Channel channel = channelRepository.findByCompanyAndExternalId(companyId, data.to()).orElse(null);
        if (channel == null) {
            log.warn("Canal não encontrado para (company={}, ref={}); ignorando", companyId, data.to());
            return;
        }
        Conversation conversation = conversationRepository
                .findByCompanyAndChannelAndPhone(companyId, channel.getId(), data.from())
                .orElseGet(() -> {
                    Contact contact = contactRepository.findByCompanyIdAndPhone(companyId, data.from()).orElse(null);
                    Conversation created = Conversation.create(companyId, channel.getId(),
                            contact != null ? contact.getId() : null, data.from());
                    return conversationRepository.save(created);
                });

        Message message = Message.createInbound(companyId, conversation.getId(), channel.getId(),
                data.from(), data.to(), data.body(), data.externalMessageId());
        // saveByExternalId: upsert ON CONFLICT(external_message_id) — idempotente no banco.
        Message persisted = messageRepository.saveByExternalId(message);

        conversation.touch(LocalDateTime.now(), true);
        conversationRepository.save(conversation);

        eventPublisher.publish(WorkflowTriggerEvent.whatsAppMessageReceived(
                companyId, conversation.getContactId(), conversation.getId(), persisted.getId(),
                data.from(), data.body()));
    }

    private void handleStatus(UUID companyId, Map<String, Object> payload) {
        WhatsAppWebhookParser.StatusData data = parser.parseStatusUpdate(payload).orElse(null);
        if (data == null) {
            log.warn("Webhook de status sem dados válidos (company={})", companyId);
            return;
        }
        messageRepository.updateStatusByExternalId(companyId, data.externalMessageId(), data.status(), data.error());
    }
}
