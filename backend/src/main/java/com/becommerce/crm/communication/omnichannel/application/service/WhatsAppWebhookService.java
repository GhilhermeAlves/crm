package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.identity.application.port.output.EventPublisher;
import com.becommerce.crm.communication.omnichannel.application.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelCompanyResolver;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelIgnoredContactRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelInboxNotifier;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppEventPublisher;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.communication.omnichannel.application.event.WhatsAppInboundEvent;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import com.becommerce.crm.automation.workflow.domain.event.WorkflowTriggerEvent;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private final WhatsAppEventPublisher whatsAppEventPublisher;
    private final JdbcTemplate jdbcTemplate;
    private final OmnichannelInboxNotifier inboxNotifier;
    private final OmnichannelIgnoredContactRepository ignoredContactRepository;
    /** Por quanto tempo a IA fica pausada após o dono do número responder pelo celular. */
    private final Duration ownerReplyPause;

    /** Janela para reconhecer o eco (fromMe) de uma mensagem que o próprio CRM enviou. */
    private static final Duration OWN_ECHO_WINDOW = Duration.ofMinutes(2);

    public WhatsAppWebhookService(WhatsAppWebhookParser parser,
                                  OmnichannelCompanyResolver companyResolver,
                                  OmnichannelChannelRepository channelRepository,
                                  OmnichannelConversationRepository conversationRepository,
                                  OmnichannelMessageRepository messageRepository,
                                  ContactRepository contactRepository,
                                  EventPublisher eventPublisher,
                                  WhatsAppEventPublisher whatsAppEventPublisher,
                                  JdbcTemplate jdbcTemplate,
                                  OmnichannelInboxNotifier inboxNotifier,
                                  OmnichannelIgnoredContactRepository ignoredContactRepository,
                                  @Value("${omnichannel.whatsapp.owner-reply-pause-hours:12}") long ownerReplyPauseHours) {
        this.ignoredContactRepository = ignoredContactRepository;
        this.ownerReplyPause = Duration.ofHours(Math.max(1, ownerReplyPauseHours));
        this.parser = parser;
        this.companyResolver = companyResolver;
        this.channelRepository = channelRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.contactRepository = contactRepository;
        this.eventPublisher = eventPublisher;
        this.whatsAppEventPublisher = whatsAppEventPublisher;
        this.jdbcTemplate = jdbcTemplate;
        this.inboxNotifier = inboxNotifier;
    }

    @Override
    @Transactional
    public void handleEvent(Map<String, Object> payload) {
        UUID companyId = null;
        try {
            String channelRef = parser.providerChannelReference(payload);
            if (channelRef == null || channelRef.isBlank()) {
                log.warn("Webhook sem referência de canal; ignorando (keys={})", payload.keySet());
                return;
            }
            log.info("Webhook recebido: channelRef={}, keys={}", channelRef, payload.keySet());
            companyId = companyResolver.resolveCompanyByChannelReference(channelRef).orElse(null);
            if (companyId == null) {
                log.warn("Webhook para canal desconhecido ({}); ignorando", channelRef);
                return;
            }
            TenantContext.setCompanyId(companyId);
            // Garante a variável de sessão do Postgres p/ as queries SECURITY DEFINER.
            jdbcTemplate.execute("SET app.current_company_id = '" + companyId + "'");

            if (parser.isInboundMessage(payload)) {
                handleInbound(companyId, payload);
            } else if (parser.isStatusUpdate(payload)) {
                handleStatus(companyId, payload);
            } else {
                log.info("Webhook sem conteúdo processável (company={})", companyId);
            }
        } catch (Exception e) {
            // Nunca logar payload/secrets; apenas ids e mensagem de erro.
            log.error("Erro ao processar webhook (company={}): {}", companyId, e.getMessage());
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
        // Contato ignorado (família/amigos no número pessoal): nada é gravado nem respondido.
        if (ignoredContactRepository.existsByCompanyAndPhone(companyId, data.from())) {
            log.info("Mensagem de contato ignorado (company={}); descartada sem gravar", companyId);
            return;
        }
        if (data.fromMe()) {
            handleOwnerMessage(companyId, channel, data);
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
        message.markType(data.type());
        // saveByExternalId: upsert ON CONFLICT(external_message_id) — idempotente no banco.
        Message persisted = messageRepository.saveByExternalId(message);

        conversation.touch(LocalDateTime.now(), true);
        conversationRepository.save(conversation);

        // Notifica a equipe só na primeira mensagem não lida da conversa —
        // evita uma notificação por mensagem enquanto ninguém abre a Inbox.
        if (conversation.getUnreadCount() == 1) {
            try {
                inboxNotifier.notifyNewInbound(companyId, conversation.getId(), data.from(), data.body());
            } catch (RuntimeException e) {
                log.warn("Falha ao notificar mensagem nova (company={}, conversation={}): {}",
                        companyId, conversation.getId(), e.getMessage());
            }
        }

        eventPublisher.publish(WorkflowTriggerEvent.whatsAppMessageReceived(
                companyId, conversation.getContactId(), conversation.getId(), persisted.getId(),
                data.from(), data.body()));

        // Sprint 23: desacopla a resposta automática do request HTTP do webhook —
        // publica o evento de inbound para a fila crm.whatsapp.inbound (AFTER_COMMIT);
        // a IA e o envio passam a ocorrer nos consumers assíncronos.
        whatsAppEventPublisher.publishInbound(WhatsAppInboundEvent.of(
                companyId, conversation.getId(), persisted.getId(), channel.getId(),
                data.externalMessageId(), data.from(), data.body(), data.senderName()));
    }

    /**
     * Mensagem que saiu do próprio número. Se não foi o CRM que a enviou, o dono
     * do número respondeu pelo celular: registra na conversa (aparece na Inbox)
     * e pausa a IA por {@link #ownerReplyPause} para não haver resposta dupla.
     * Conversas que não existem no CRM (contatos pessoais) não são criadas.
     */
    private void handleOwnerMessage(UUID companyId, Channel channel,
                                    WhatsAppWebhookParser.InboundMessageData data) {
        Conversation conversation = conversationRepository
                .findByCompanyAndChannelAndPhone(companyId, channel.getId(), data.from())
                .orElse(null);
        if (conversation == null) {
            log.debug("Mensagem própria para conversa fora do CRM (company={}); ignorada", companyId);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        // Eco de envio do CRM antes de o id externo ser gravado (corrida com o sender).
        // Nota de voz enviada pelo CRM chega como áudio (sem o texto): qualquer
        // OUTBOUND recente na conversa indica que o eco é nosso.
        boolean echo = data.type() == MessageType.AUDIO
                ? messageRepository.existsOutboundAfter(conversation.getId(), now.minus(OWN_ECHO_WINDOW))
                : messageRepository.existsOutboundWithBodyAfter(conversation.getId(), data.body(),
                        now.minus(OWN_ECHO_WINDOW));
        if (echo) {
            log.debug("Eco de mensagem enviada pelo CRM (company={}, conversation={})",
                    companyId, conversation.getId());
            return;
        }
        Message manual = Message.createOutbound(companyId, conversation.getId(), channel.getId(),
                channel.getExternalId(), data.from(), data.body(), UUID.randomUUID());
        manual.markSent(data.externalMessageId());
        messageRepository.saveByExternalId(manual);

        conversation.pauseAutomationUntil(now.plus(ownerReplyPause));
        conversation.touch(now, false);
        conversationRepository.save(conversation);
        log.info("[WHATSAPP][OWNER] company={} conversation={} resposta pelo celular; IA pausada até {}",
                companyId, conversation.getId(), conversation.getHumanUntil());
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
