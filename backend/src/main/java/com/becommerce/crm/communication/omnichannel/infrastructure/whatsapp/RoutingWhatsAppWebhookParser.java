package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Escolhe o parser pelo formato do payload: Meta ({@code object=whatsapp_business_account})
 * ou Evolution (demais). Permite canais dos dois providers no mesmo webhook.
 */
@Primary
@Component
public class RoutingWhatsAppWebhookParser implements WhatsAppWebhookParser {

    private final EvolutionWebhookParser evolution;
    private final MetaWhatsAppWebhookParser meta;

    public RoutingWhatsAppWebhookParser(EvolutionWebhookParser evolution, MetaWhatsAppWebhookParser meta) {
        this.evolution = evolution;
        this.meta = meta;
    }

    private WhatsAppWebhookParser of(Map<String, Object> raw) {
        return MetaWhatsAppWebhookParser.isMetaPayload(raw) ? meta : evolution;
    }

    @Override
    public List<Map<String, Object>> splitEvents(Map<String, Object> raw) {
        return of(raw).splitEvents(raw);
    }

    @Override
    public boolean isInboundMessage(Map<String, Object> raw) {
        return of(raw).isInboundMessage(raw);
    }

    @Override
    public Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw) {
        return of(raw).parseInboundMessage(raw);
    }

    @Override
    public String inboundSkipReason(Map<String, Object> raw) {
        return of(raw).inboundSkipReason(raw);
    }

    /** Motivos esperados de ambos os providers. */
    @Override
    public boolean isExpectedSkip(String reason) {
        return evolution.isExpectedSkip(reason) || meta.isExpectedSkip(reason);
    }

    @Override
    public boolean isStatusUpdate(Map<String, Object> raw) {
        return of(raw).isStatusUpdate(raw);
    }

    @Override
    public Optional<StatusData> parseStatusUpdate(Map<String, Object> raw) {
        return of(raw).parseStatusUpdate(raw);
    }

    @Override
    public String providerChannelReference(Map<String, Object> raw) {
        return of(raw).providerChannelReference(raw);
    }

    @Override
    public String providerName() {
        return "ROUTING";
    }
}
