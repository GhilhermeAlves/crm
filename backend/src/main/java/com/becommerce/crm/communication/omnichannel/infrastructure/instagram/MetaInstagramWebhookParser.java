package com.becommerce.crm.communication.omnichannel.infrastructure.instagram;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Normaliza webhooks do Instagram Direct (Instagram API com login do Instagram).
 *
 * <p>Formato: {@code {"object":"instagram","entry":[{"id":"<IG_ID>","time":…,
 * "messaging":[{"sender":{"id":"<IGSID>"},"recipient":{"id":"<IG_ID>"},"timestamp":…,
 * "message":{"mid":"…","text":"…","attachments":[…],"is_echo":true?}}]}]}}.
 *
 * <p>Mapeamento para o pipeline do omnichannel (o mesmo do WhatsApp):
 * <ul>
 *   <li>referência de canal e {@code to} = {@code entry.id} (conta profissional = {@code externalId} do canal);</li>
 *   <li>{@code from} = IGSID do cliente (ocupa o lugar do telefone na conversa);</li>
 *   <li>{@code is_echo} = mensagem enviada pela própria conta ({@code fromMe}): o
 *       remetente é a conta e o cliente é o {@code recipient};</li>
 *   <li>postbacks (botões) viram texto com o título do botão.</li>
 * </ul>
 * Eventos sem mensagem (lido, reação, comentários) não geram evento: o Instagram
 * não tem status de entrega por mensagem como o WhatsApp.
 */
@Component
public class MetaInstagramWebhookParser implements WhatsAppWebhookParser {

    static final String OBJECT = "instagram";

    /** O payload veio do webhook do Instagram? */
    public static boolean isInstagramPayload(Map<String, Object> raw) {
        return raw != null && OBJECT.equals(str(raw.get("object")));
    }

    @Override
    public List<Map<String, Object>> splitEvents(Map<String, Object> raw) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (Object entry : list(raw.get("entry"))) {
            String accountId = str(map(entry).get("id"));
            for (Object item : list(map(entry).get("messaging"))) {
                Map<?, ?> messaging = map(item);
                if (messaging.get("message") != null || messaging.get("postback") != null) {
                    events.add(Map.of("object", OBJECT,
                            "entry", List.of(Map.of("id", accountId == null ? "" : accountId,
                                    "messaging", List.of(messaging)))));
                }
            }
        }
        return events;
    }

    @Override
    public boolean isInboundMessage(Map<String, Object> raw) {
        Map<?, ?> messaging = messaging(raw);
        return messaging.get("message") != null || messaging.get("postback") != null;
    }

    @Override
    public Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw) {
        Map<?, ?> messaging = messaging(raw);
        Map<?, ?> message = map(messaging.get("message"));
        Map<?, ?> postback = map(messaging.get("postback"));
        String accountId = accountId(raw);
        boolean echo = Boolean.TRUE.equals(message.get("is_echo"));
        String customer = str(map(messaging.get(echo ? "recipient" : "sender")).get("id"));
        String id = message.isEmpty() ? str(postback.get("mid")) : str(message.get("mid"));
        if (id == null || customer == null || accountId == null || customer.equals(accountId)) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(message.get("is_deleted")) || Boolean.TRUE.equals(message.get("is_unsupported"))) {
            return Optional.empty();
        }
        String body = message.isEmpty() ? str(postback.get("title")) : text(message);
        return Optional.of(new InboundMessageData(id, customer, accountId,
                body == null ? "[mensagem]" : body, null, echo, mediaType(message)));
    }

    @Override
    public String inboundSkipReason(Map<String, Object> raw) {
        Map<?, ?> messaging = messaging(raw);
        Map<?, ?> message = map(messaging.get("message"));
        if (Boolean.TRUE.equals(message.get("is_deleted"))) {
            return "apagada";
        }
        if (Boolean.TRUE.equals(message.get("is_unsupported"))) {
            return "nao-suportada";
        }
        if (accountId(raw) == null) {
            return "sem-conta";
        }
        return str(map(messaging.get("sender")).get("id")) == null ? "sem-remetente" : "desconhecido";
    }

    @Override
    public boolean isExpectedSkip(String reason) {
        return "apagada".equals(reason) || "nao-suportada".equals(reason);
    }

    @Override
    public boolean isStatusUpdate(Map<String, Object> raw) {
        return false;
    }

    @Override
    public Optional<StatusData> parseStatusUpdate(Map<String, Object> raw) {
        return Optional.empty();
    }

    @Override
    public String providerChannelReference(Map<String, Object> raw) {
        return accountId(raw);
    }

    @Override
    public String providerName() {
        return "META_INSTAGRAM";
    }

    private static String text(Map<?, ?> message) {
        String text = str(message.get("text"));
        if (text != null) {
            return text;
        }
        Map<?, ?> attachment = first(message.get("attachments"));
        String type = str(attachment.get("type"));
        if ("story_mention".equals(type)) {
            return "[mencionou você no story]";
        }
        if (map(message.get("reply_to")).get("story") != null) {
            return "[respondeu ao story]";
        }
        return type == null ? null : "[" + type + "]";
    }

    private static MessageType mediaType(Map<?, ?> message) {
        if (str(message.get("text")) != null) {
            return MessageType.TEXT;
        }
        String type = str(first(message.get("attachments")).get("type"));
        if ("audio".equals(type)) {
            return MessageType.AUDIO;
        }
        if ("image".equals(type)) {
            return MessageType.IMAGE;
        }
        if ("file".equals(type)) {
            return MessageType.DOCUMENT;
        }
        return MessageType.TEXT;
    }

    private static Map<?, ?> messaging(Map<String, Object> raw) {
        return first(first(raw.get("entry")).get("messaging"));
    }

    private static String accountId(Map<String, Object> raw) {
        return str(first(raw.get("entry")).get("id"));
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
}
