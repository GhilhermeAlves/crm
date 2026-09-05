package com.becommerce.crm.infrastructure.omnichannel;

import com.becommerce.crm.application.omnichannel.service.WhatsAppInboundAutoReplyProcessor;
import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

/**
 * Consome o evento {@code WHATSAPP_MESSAGE_RECEIVED} (publicado pelo webhook via
 * {@code EventPublisher}) e delega ao {@link WhatsAppInboundAutoReplyProcessor}.
 *
 * <p>{@code AFTER_COMMIT}: a auto-resposta (IA/WhatsApp são chamadas HTTP longas)
 * NUNCA roda dentro da transação do webhook — o commit já aconteceu quando o
 * listener executa, e o processor gerencia o {@code TenantContext} próprio (o
 * webhook limpa o dele no {@code finally} antes do commit).</p>
 */
@Component
public class WhatsAppInboundAutoReplyEventListener {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppInboundAutoReplyEventListener.class);

    private final WhatsAppInboundAutoReplyProcessor processor;

    public WhatsAppInboundAutoReplyEventListener(WhatsAppInboundAutoReplyProcessor processor) {
        this.processor = processor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInboundMessage(WorkflowTriggerEvent event) {
        if (event.trigger() != TriggerEvent.WHATSAPP_MESSAGE_RECEIVED) {
            return;
        }
        try {
            Map<String, Object> ctx = event.context();
            Object rawConversationId = ctx.get("whatsapp.conversationId");
            String from = ctx.get("whatsapp.from") != null ? ctx.get("whatsapp.from").toString() : null;
            String body = ctx.get("whatsapp.body") != null ? ctx.get("whatsapp.body").toString() : null;
            if (rawConversationId == null || from == null || body == null || event.eventId() == null) {
                log.warn("Evento WHATSAPP_MESSAGE_RECEIVED sem contexto completo; ignorando auto-resposta");
                return;
            }
            processor.processInbound(event.companyId(),
                    UUID.fromString(rawConversationId.toString()), event.eventId(), from, body);
        } catch (Exception e) {
            // A falha da auto-resposta não pode afetar o webhook já confirmado.
            log.error("Erro no processamento de auto-resposta (company={}): {}",
                    event.companyId(), e.getMessage());
        }
    }
}