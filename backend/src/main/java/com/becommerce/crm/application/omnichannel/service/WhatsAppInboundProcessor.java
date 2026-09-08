package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.omnichannel.event.WhatsAppAutoAiEvent;
import com.becommerce.crm.application.omnichannel.event.WhatsAppInboundEvent;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppEventPublisher;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Consumer lógico da fila {@code crm.whatsapp.inbound} (Sprint 23). Executa
 * FORA do request HTTP do webhook (que agora apenas persiste e publica).
 *
 * <p>Responsabilidade única: revalidar o estado da conversa e DECIDIR o próximo
 * passo. Sempre reexecutado sob o TenantContext da empresa do evento
 * (nunca confia em ThreadLocal/request anterior) e limpo em {@code finally}.
 *
 * <ul>
 *   <li>evento inválido / sem {@code companyId} → rejeita com segurança (log, sem retry);</li>
 *   <li>conversa inexistente ou de outra empresa → log + descarte;</li>
 *   <li>{@code ConversationMode.HUMAN} → NÃO publica evento de IA (preserva Human Takeover);</li>
 *   <li>caso contrário → publica {@link WhatsAppAutoAiEvent}.
 * </ul>
 */
@Service
public class WhatsAppInboundProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppInboundProcessor.class);

    private final OmnichannelConversationRepository conversationRepository;
    private final WhatsAppEventPublisher eventPublisher;

    public WhatsAppInboundProcessor(OmnichannelConversationRepository conversationRepository,
                                    WhatsAppEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(WhatsAppInboundEvent event) {
        if (event == null || event.companyId() == null || event.conversationId() == null
                || event.messageId() == null) {
            log.warn("[WHATSAPP][INBOUND] evento inválido; rejeitado com segurança");
            return;
        }
        UUID companyId = event.companyId();
        TenantContext.setCompanyId(companyId);
        try {
            Conversation conversation = conversationRepository.findById(event.conversationId()).orElse(null);
            if (conversation == null || !conversation.getCompanyId().equals(companyId)) {
                log.warn("[WHATSAPP][INBOUND] eventId={} companyId={} conversationId={} não encontrada; descartando",
                        event.eventId(), companyId, event.conversationId());
                return;
            }
            if (conversation.isInHumanMode()) {
                log.info("[WHATSAPP][INBOUND] eventId={} companyId={} conversationId={} HUMAN; IA não acionada",
                        event.eventId(), companyId, event.conversationId());
                return;
            }
            eventPublisher.publishAutoAi(WhatsAppAutoAiEvent.of(companyId, event.conversationId(),
                    event.messageId(), event.from(), event.body()));
            log.info("[WHATSAPP][INBOUND] eventId={} companyId={} conversationId={} roteado para IA",
                    event.eventId(), companyId, event.conversationId());
        } finally {
            TenantContext.clear();
        }
    }
}