package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppWebhookParser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compositor que detecta o formato do payload (Meta Cloud API vs UAZAPI)
 * e delega para o parser correto.
 *
 * <p>Detecção:
 * <ul>
 *   <li>UAZAPI: payload contém {@code EventType} e {@code message}</li>
 *   <li>Meta Cloud API: payload contém {@code entry} (lista)</li>
 * </ul>
 */
@Component
public class CompositeWhatsAppWebhookParser implements WhatsAppWebhookParser {

    private final List<WhatsAppWebhookParser> parsers;

    public CompositeWhatsAppWebhookParser(UazapiWebhookParser uazapiParser,
                                          WhatsAppCloudApiWebhookParser cloudApiParser) {
        this.parsers = List.of(uazapiParser, cloudApiParser);
    }

    @Override
    public boolean isInboundMessage(Map<String, Object> raw) {
        return resolveParser(raw).isInboundMessage(raw);
    }

    @Override
    public Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw) {
        return resolveParser(raw).parseInboundMessage(raw);
    }

    @Override
    public boolean isStatusUpdate(Map<String, Object> raw) {
        return resolveParser(raw).isStatusUpdate(raw);
    }

    @Override
    public Optional<StatusData> parseStatusUpdate(Map<String, Object> raw) {
        return resolveParser(raw).parseStatusUpdate(raw);
    }

    @Override
    public Verification parseVerification(Map<String, String> params) {
        // GET verification é sempre Meta (UAZAPI não usa)
        return new WhatsAppCloudApiWebhookParser().parseVerification(params);
    }

    @Override
    public String providerChannelReference(Map<String, Object> raw) {
        return resolveParser(raw).providerChannelReference(raw);
    }

    @Override
    public String providerName() {
        return "COMPOSITE";
    }

    /**
     * Resolve o parser correto baseado no formato do payload.
     * UAZAPI tem prioridade (mais específico).
     */
    private WhatsAppWebhookParser resolveParser(Map<String, Object> raw) {
        for (WhatsAppWebhookParser parser : parsers) {
            if (parser instanceof UazapiWebhookParser && UazapiWebhookParser.isUazapiFormat(raw)) {
                return parser;
            }
            if (parser instanceof WhatsAppCloudApiWebhookParser && !UazapiWebhookParser.isUazapiFormat(raw)) {
                return parser;
            }
        }
        // Fallback: Meta Cloud API (formato padrão)
        return parsers.get(1);
    }
}
