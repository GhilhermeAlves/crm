package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Normaliza webhooks do WhatsApp Cloud API (Meta).
 *
 * <p>Formato: {@code {"object":"whatsapp_business_account","entry":[{"changes":[{"field":"messages",
 * "value":{"metadata":{"phone_number_id":"…"},"contacts":[…],"messages":[…],"statuses":[…]}}]}]}}.
 * Um POST pode trazer várias mensagens/status: {@link #splitEvents} devolve um
 * payload no mesmo formato com exatamente UM item, e os demais métodos leem o
 * primeiro item. A referência de canal é {@code metadata.phone_number_id}
 * (= {@code externalId} do canal). A Cloud API não ecoa mensagens enviadas
 * pelo próprio número, então {@code fromMe} é sempre {@code false}.
 */
@Component
public class MetaWhatsAppWebhookParser implements WhatsAppWebhookParser {

    static final String OBJECT = "whatsapp_business_account";

    /** O payload veio da Meta (e não da Evolution)? */
    public static boolean isMetaPayload(Map<String, Object> raw) {
        return raw != null && OBJECT.equals(str(raw.get("object")));
    }

    @Override
    public List<Map<String, Object>> splitEvents(Map<String, Object> raw) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (Object entry : list(raw.get("entry"))) {
            for (Object change : list(map(entry).get("changes"))) {
                Map<?, ?> value = map(map(change).get("value"));
                for (Object message : list(value.get("messages"))) {
                    events.add(single(value, "messages", message));
                }
                for (Object status : list(value.get("statuses"))) {
                    events.add(single(value, "statuses", status));
                }
            }
        }
        return events;
    }

    @Override
    public boolean isInboundMessage(Map<String, Object> raw) {
        return !list(value(raw).get("messages")).isEmpty();
    }

    @Override
    public Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw) {
        Map<?, ?> value = value(raw);
        Map<?, ?> message = first(value.get("messages"));
        String id = str(message.get("id"));
        String from = digits(str(message.get("from")));
        String phoneNumberId = phoneNumberId(value);
        if (id == null || from == null || phoneNumberId == null) {
            return Optional.empty();
        }
        String senderName = str(map(first(value.get("contacts")).get("profile")).get("name"));
        return Optional.of(new InboundMessageData(id, from, phoneNumberId, text(message), senderName, false,
                mediaType(message)));
    }

    @Override
    public String inboundSkipReason(Map<String, Object> raw) {
        Map<?, ?> value = value(raw);
        Map<?, ?> message = first(value.get("messages"));
        if (message.isEmpty()) {
            return "sem-mensagem";
        }
        if (str(message.get("id")) == null) {
            return "sem-id";
        }
        if (digits(str(message.get("from"))) == null) {
            return "sem-remetente";
        }
        return phoneNumberId(value) == null ? "sem-phone-number-id" : "desconhecido";
    }

    @Override
    public boolean isStatusUpdate(Map<String, Object> raw) {
        return !list(value(raw).get("statuses")).isEmpty();
    }

    @Override
    public Optional<StatusData> parseStatusUpdate(Map<String, Object> raw) {
        Map<?, ?> status = first(value(raw).get("statuses"));
        String id = str(status.get("id"));
        MessageStatus mapped = mapStatus(str(status.get("status")));
        if (id == null || mapped == null) {
            return Optional.empty();
        }
        return Optional.of(new StatusData(id, mapped, mapped == MessageStatus.FAILED ? error(status) : null));
    }

    @Override
    public String providerChannelReference(Map<String, Object> raw) {
        return phoneNumberId(value(raw));
    }

    @Override
    public String providerName() {
        return "META_CLOUD";
    }

    /** Payload no formato da Meta com um único item em {@code listKey}. */
    private static Map<String, Object> single(Map<?, ?> value, String listKey, Object item) {
        Map<String, Object> one = new LinkedHashMap<>();
        one.put("metadata", value.get("metadata"));
        if (value.get("contacts") != null) {
            one.put("contacts", value.get("contacts"));
        }
        one.put(listKey, List.of(item));
        return Map.of("object", OBJECT,
                "entry", List.of(Map.of("changes", List.of(Map.of("field", "messages", "value", one)))));
    }

    /** {@code value} da primeira mudança do primeiro entry. */
    private static Map<?, ?> value(Map<String, Object> raw) {
        Map<?, ?> change = first(first(raw.get("entry")).get("changes"));
        return map(change.get("value"));
    }

    private static String phoneNumberId(Map<?, ?> value) {
        return str(map(value.get("metadata")).get("phone_number_id"));
    }

    private static String text(Map<?, ?> message) {
        String type = str(message.get("type"));
        Map<?, ?> interactive = map(message.get("interactive"));
        String body = firstNonNull(
                str(map(message.get("text")).get("body")),
                str(map(message.get("image")).get("caption")),
                str(map(message.get("video")).get("caption")),
                str(map(message.get("document")).get("caption")),
                str(map(message.get("button")).get("text")),
                str(map(interactive.get("button_reply")).get("title")),
                str(map(interactive.get("list_reply")).get("title")));
        if (body != null) {
            return body;
        }
        return "[" + (type != null ? type : "mensagem") + "]";
    }

    /** Áudio, foto e PDF são entendidos pelo agente; demais mídias ficam como texto/placeholder. */
    private static MessageType mediaType(Map<?, ?> message) {
        String type = str(message.get("type"));
        if ("audio".equals(type)) {
            return MessageType.AUDIO;
        }
        if ("image".equals(type)) {
            return MessageType.IMAGE;
        }
        String mime = str(map(message.get("document")).get("mime_type"));
        if ("document".equals(type) && mime != null && mime.toLowerCase(Locale.ROOT).contains("pdf")) {
            return MessageType.DOCUMENT;
        }
        return MessageType.TEXT;
    }

    private static MessageStatus mapStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "sent" -> MessageStatus.SENT;
            case "delivered" -> MessageStatus.DELIVERED;
            case "read" -> MessageStatus.READ;
            case "failed" -> MessageStatus.FAILED;
            default -> null;
        };
    }

    private static String error(Map<?, ?> status) {
        Map<?, ?> err = first(status.get("errors"));
        String code = str(err.get("code"));
        String title = firstNonNull(str(err.get("title")), str(err.get("message")));
        if (code == null && title == null) {
            return "Meta reportou failed";
        }
        return "Meta" + (code != null ? " " + code : "") + (title != null ? " " + title : "");
    }

    private static String digits(String phone) {
        if (phone == null) {
            return null;
        }
        String d = phone.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    private static Map<?, ?> first(Object o) {
        List<?> l = list(o);
        return l.isEmpty() ? Map.of() : map(l.get(0));
    }

    private static List<?> list(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }

    private static Map<?, ?> map(Object o) {
        return o instanceof Map<?, ?> m ? m : Map.of();
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o);
        return s.isBlank() ? null : s;
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
