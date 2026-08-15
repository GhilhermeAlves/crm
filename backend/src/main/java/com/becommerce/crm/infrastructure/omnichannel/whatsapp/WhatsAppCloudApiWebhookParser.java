package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parser dos webhooks da WhatsApp Cloud API (Meta), FASE 6/10/17.
 * Desacopla o serviço da estrutura do provider.
 *
 * <p>Estruturas reconhecidas (padrão Meta):
 * <ul>
 *   <li>Entrada: {@code entry[].changes[].value.messages[0]} com
 *       {@code .from}, {@code .text.body} e {@code .id} (wamid).</li>
 *   <li>Status: {@code entry[].changes[].value.statuses[0]} com
 *       {@code .id} e {@code .status} (sent/delivered/read/failed) e
 *       {@code .errors[0].message}.</li>
 *   <li>Referência do canal: {@code value.metadata.phone_number_id}.</li>
 * </ul>
 * Em simulação/fake o {@code phone_number_id} da empresa é usado como a
 * referência do canal (mesmo formato do prod).
 */
@Component
public class WhatsAppCloudApiWebhookParser implements WhatsAppWebhookParser {

    @Override
    public boolean isInboundMessage(Map<String, Object> raw) {
        return channelValue(raw).flatMap(v -> map(v, "messages"))
                .filter(l -> !((List<?>) l).isEmpty())
                .isPresent();
    }

    @Override
    public Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw) {
        List<?> messages = channelValue(raw).flatMap(v -> map(v, "messages"))
                .map(l -> (List<?>) l).orElse(List.of());
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        Map<?, ?> message = (Map<?, ?>) messages.get(0);
        String externalId = stringField(message, "id");
        String from = stringField(message, "from");
        String body = messageBody(message);
        Map<?, ?> metadata = metadata(raw).orElse(Map.of());
        String to = stringField(metadata, "phone_number_id");
        if (externalId == null || from == null) {
            return Optional.empty();
        }
        return Optional.of(new InboundMessageData(externalId, from, to, body));
    }

    @Override
    public boolean isStatusUpdate(Map<String, Object> raw) {
        return channelValue(raw).flatMap(v -> map(v, "statuses"))
                .filter(l -> !((List<?>) l).isEmpty())
                .isPresent();
    }

    @Override
    public Optional<StatusData> parseStatusUpdate(Map<String, Object> raw) {
        List<?> statuses = channelValue(raw).flatMap(v -> map(v, "statuses"))
                .map(l -> (List<?>) l).orElse(List.of());
        if (statuses.isEmpty()) {
            return Optional.empty();
        }
        Map<?, ?> status = (Map<?, ?>) statuses.get(0);
        String externalId = stringField(status, "id");
        MessageStatus mapped = mapStatus(stringField(status, "status"));
        if (externalId == null || mapped == null) {
            return Optional.empty();
        }
        String error = errorsMessage(status);
        return Optional.of(new StatusData(externalId, mapped, error));
    }

    @Override
    public Verification parseVerification(Map<String, String> params) {
        return new Verification(
                params.get("hub.mode"),
                params.get("hub.verify_token"),
                params.get("hub.challenge"));
    }

    @Override
    public String providerChannelReference(Map<String, Object> raw) {
        return metadata(raw).flatMap(m -> Optional.ofNullable(stringField(m, "phone_number_id"))).orElse(null);
    }

    @Override
    public String providerName() {
        return "WHATSAPP_CLOUD_API";
    }

    // ------------------------------------------------------------------
    // Helpers de navegação tolerante a tipos.
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Optional<Map<String, Object>> channelValue(Map<String, Object> raw) {
        List<?> entries = raw.get("entry") instanceof List<?> e ? e : List.of();
        for (Object entryObj : entries) {
            if (!(entryObj instanceof Map<?, ?> entry)) {
                continue;
            }
            List<?> changes = entry.get("changes") instanceof List<?> c ? c : List.of();
            for (Object changeObj : changes) {
                if (!(changeObj instanceof Map<?, ?> change)) {
                    continue;
                }
                Object value = change.get("value");
                if (value instanceof Map<?, ?> valueMap) {
                    Map<String, Object> casted = (Map<String, Object>) valueMap;
                    return Optional.of(casted);
                }
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static Optional<Map<String, Object>> metadata(Map<String, Object> raw) {
        return channelValue(raw).flatMap(v -> {
            Object m = v.get("metadata");
            return m instanceof Map<?, ?> mm ? Optional.of((Map<String, Object>) mm) : Optional.empty();
        });
    }

    private static Optional<Object> map(Map<String, Object> map, String key) {
        return Optional.ofNullable(map.get(key));
    }

    @SuppressWarnings("unchecked")
    private static String stringField(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof String s ? s : null;
    }

    private static String messageBody(Map<?, ?> message) {
        Object text = message.get("text");
        if (text instanceof Map<?, ?> textMap) {
            String body = stringField(textMap, "body");
            return body != null ? body : "";
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static String errorsMessage(Map<?, ?> status) {
        Object errors = status.get("errors");
        if (errors instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> errorMap) {
                return stringField(errorMap, "message");
            }
        }
        return null;
    }

    private static MessageStatus mapStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toLowerCase()) {
            case "sent" -> MessageStatus.SENT;
            case "delivered" -> MessageStatus.DELIVERED;
            case "read" -> MessageStatus.READ;
            case "failed" -> MessageStatus.FAILED;
            default -> null;
        };
    }
}
