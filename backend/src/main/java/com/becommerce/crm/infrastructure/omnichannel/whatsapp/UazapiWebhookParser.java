package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Parser dos webhooks da UAZAPI (uazapiGO V2).
 * Formato UAZAPI: {@code {EventType, message: {chatid, messageid, text, fromMe, ...}, chat: {...}}}.
 *
 * <p>Diferente do Meta Cloud API, a UAZAPI envia:
 * <ul>
 *   <li>{@code message.chatid} no lugar de {@code entry[].changes[].value.messages[0].from}</li>
 *   <li>{@code message.messageid} no lugar de {@code ...messages[0].id}</li>
 *   <li>{@code message.text} no lugar de {@code ...messages[0].text.body}</li>
 *   <li>{@code owner} (número da instância) como referência do canal</li>
 * </ul>
 */
/**
 * Parser dos webhooks da UAZAPI (uazapiGO V2).
 * Detecta e parseia o formato UAZAPI: {@code {EventType, message: {chatid, messageid, text, fromMe, ...}, chat: {...}}}.
 *
 * <p>Diferente do Meta Cloud API, a UAZAPI envia:
 * <ul>
 *   <li>{@code message.chatid} no lugar de {@code entry[].changes[].value.messages[0].from}</li>
 *   <li>{@code message.messageid} no lugar de {@code ...messages[0].id}</li>
 *   <li>{@code message.text} no lugar de {@code ...messages[0].text.body}</li>
 *   <li>{@code owner} (número da instância) como referência do canal</li>
 * </ul>
 */
@Component
public class UazapiWebhookParser implements WhatsAppWebhookParser {

    private static final Logger log = LoggerFactory.getLogger(UazapiWebhookParser.class);

    @Override
    public boolean isInboundMessage(Map<String, Object> raw) {
        if (!isUazapiFormat(raw)) {
            return false;
        }
        Object eventObj = raw.get("event");
        if (eventObj instanceof String event && !"messages".equals(event)) {
            return false;
        }
        Object message = getMessageFromPayload(raw);
        if (!(message instanceof Map<?, ?> msg)) {
            return false;
        }
        Boolean fromMe = getBoolean(msg, "fromMe");
        Boolean wasSentByApi = getBoolean(msg, "wasSentByApi");
        return Boolean.FALSE.equals(fromMe) && Boolean.FALSE.equals(wasSentByApi);
    }

    @Override
    public Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw) {
        if (!isUazapiFormat(raw)) {
            return Optional.empty();
        }
        Map<?, ?> message = getMessageMap(getMessageFromPayload(raw));
        if (message == null) {
            return Optional.empty();
        }
        String externalId = stringField(message, "messageid");
        if (externalId == null) {
            externalId = extractMessageIdFromData(raw);
        }
        String from = extractSender(message);
        if (from == null) {
            from = extractSenderFromData(raw);
        }
        String to = extractTo(raw);
        String body = extractBody(message);
        log.info("[UAZAPI-PARSE] from={} to={} body={} extId={} | data={}", from, to, body, externalId, raw.get("data"));
        if (externalId == null || from == null) {
            return Optional.empty();
        }
        return Optional.of(new InboundMessageData(externalId, from, to, body));
    }

    @Override
    public boolean isStatusUpdate(Map<String, Object> raw) {
        if (!isUazapiFormat(raw)) {
            return false;
        }
        String eventType = getEventType(raw);
        return "messages_update".equals(eventType) || "connection".equals(eventType);
    }

    @Override
    public Optional<StatusData> parseStatusUpdate(Map<String, Object> raw) {
        if (!isUazapiFormat(raw)) {
            return Optional.empty();
        }
        Map<?, ?> message = getMessageMap(getMessageFromPayload(raw));
        if (message == null) {
            return Optional.empty();
        }
        String externalId = stringField(message, "messageid");
        String statusStr = stringField(message, "status");
        if (externalId == null || statusStr == null) {
            return Optional.empty();
        }
        MessageStatus mapped = mapStatus(statusStr);
        if (mapped == null) {
            return Optional.empty();
        }
        return Optional.of(new StatusData(externalId, mapped, null));
    }

    @Override
    public Verification parseVerification(Map<String, String> params) {
        // UAZAPI não usa verificação GET como o Meta
        return new Verification(null, null, null);
    }

    @Override
    public String providerChannelReference(Map<String, Object> raw) {
        if (!isUazapiFormat(raw)) {
            return null;
        }
        return extractTo(raw);
    }

    @Override
    public String providerName() {
        return "UAZAPI";
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Detecta se o payload é no formato UAZAPI.
     * Suporta dois formatos:
     * <ul>
     *   <li>V1 (legado): {@code EventType} + {@code message} no topo</li>
     *   <li>V2 (uazapiGO): {@code event} + {@code instance} + {@code data} no topo</li>
     * </ul>
     */
    static boolean isUazapiFormat(Map<String, Object> raw) {
        if (raw == null) {
            return false;
        }
        if (raw.containsKey("EventType")) {
            return true;
        }
        return raw.containsKey("event") && raw.containsKey("instance");
    }

    private static Map<?, ?> getMessageMap(Object messageObj) {
        return messageObj instanceof Map<?, ?> msg ? msg : null;
    }

    /**
     * Extrai o mapa de mensagem do payload, suportando V1 (top-level) e V2 (dentro de "data").
     */
    private static Object getMessageFromPayload(Map<String, Object> raw) {
        // V1: message direto no topo
        Object message = raw.get("message");
        if (message instanceof Map<?, ?>) {
            return message;
        }
        // V2: data contém o payload do evento
        Object data = raw.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            // Caso 1: data.message existe (formato wrapper)
            Object nested = dataMap.get("message");
            if (nested instanceof Map<?, ?>) {
                return nested;
            }
            // Caso 2: data é diretamente a mensagem (campos como chatid, text, etc.)
            if (dataMap.containsKey("chatid") || dataMap.containsKey("messageid")
                    || dataMap.containsKey("text") || dataMap.containsKey("fromMe")) {
                return dataMap;
            }
        }
        return null;
    }

    /**
     * Extrai o tipo do evento, suportando V1 ("EventType") e V2 ("event").
     */
    private static String getEventType(Map<String, Object> raw) {
        String v1 = stringField(raw, "EventType");
        if (v1 != null) {
            return v1;
        }
        return stringField(raw, "event");
    }

    /**
     * Extrai o destinatário (owner/number da instância) do payload V1 ou V2.
     * V1: {@code owner} no topo.
     * V2: {@code data.owner} ou {@code instance} no topo.
     */
    private static String extractTo(Map<String, Object> raw) {
        String owner = stringField(raw, "owner");
        if (owner != null && !owner.isBlank()) {
            return owner;
        }
        Object data = raw.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            String dataOwner = stringField(dataMap, "owner");
            if (dataOwner != null && !dataOwner.isBlank()) {
                return dataOwner;
            }
        }
        return stringField(raw, "instance");
    }

    /**
     * Extrai messageid do V2 data quando não está em data.message.
     */
    private static String extractMessageIdFromData(Map<String, Object> raw) {
        Object data = raw.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object key = dataMap.get("key");
            if (key instanceof Map<?, ?> keyMap) {
                return stringField(keyMap, "id");
            }
        }
        return null;
    }

    /**
     * Extrai o sender do V2 data quando não está em data.message.
     */
    private static String extractSenderFromData(Map<String, Object> raw) {
        Object data = raw.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object key = dataMap.get("key");
            if (key instanceof Map<?, ?> keyMap) {
                String remoteJid = stringField(keyMap, "remoteJid");
                if (remoteJid != null && !remoteJid.isBlank()) {
                    return stripSuffix(remoteJid);
                }
            }
            String sender = stringField(dataMap, "sender");
            if (sender != null && !sender.isBlank()) {
                return stripSuffix(sender);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String extractSender(Map<?, ?> message) {
        // Fallback chain: sender -> sender_pn -> sender_lid -> chatid
        String sender = stringField(message, "sender");
        if (sender != null && !sender.isBlank()) {
            return stripSuffix(sender);
        }
        String senderPn = stringField(message, "sender_pn");
        if (senderPn != null && !senderPn.isBlank()) {
            return stripSuffix(senderPn);
        }
        String senderLid = stringField(message, "sender_lid");
        if (senderLid != null && !senderLid.isBlank()) {
            return stripSuffix(senderLid);
        }
        String chatid = stringField(message, "chatid");
        if (chatid != null && !chatid.isBlank()) {
            return stripSuffix(chatid);
        }
        return null;
    }

    private static String extractBody(Map<?, ?> message) {
        // Para textos: message.text
        Object text = message.get("text");
        if (text instanceof String s && !s.isBlank()) {
            return s;
        }
        // Para mídia: message.content pode ser string ou objeto
        Object content = message.get("content");
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof Map<?, ?> contentMap) {
            Object caption = contentMap.get("caption");
            if (caption instanceof String s) {
                return s;
            }
        }
        return "";
    }

    /**
     * Remove o sufixo @s.whatsapp.net ou @g.us do JID.
     */
    private static String stripSuffix(String jid) {
        if (jid == null) {
            return null;
        }
        int atIndex = jid.indexOf('@');
        return atIndex > 0 ? jid.substring(0, atIndex) : jid;
    }

    @SuppressWarnings("unchecked")
    private static Optional<Object> map(Map<String, Object> map, String key) {
        return Optional.ofNullable(map.get(key));
    }

    @SuppressWarnings("unchecked")
    private static String stringField(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof String s ? s : null;
    }

    private static Boolean getBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
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
