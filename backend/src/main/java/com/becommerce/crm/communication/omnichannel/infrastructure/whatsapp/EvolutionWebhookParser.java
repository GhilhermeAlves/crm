package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Normaliza webhooks da Evolution API v2.
 *
 * <p>Formato: {@code {"event":"messages.upsert","instance":"<nome>","data":{…}}}.
 * <ul>
 *   <li>{@code messages.upsert}: mensagem recebida em {@code data.key} / {@code data.message};</li>
 *   <li>{@code messages.update}: status em {@code data.keyId} / {@code data.status}.</li>
 * </ul>
 * A referência de canal é o nome da instância (= {@code externalId} do canal).
 * Mensagens próprias ({@code fromMe}) são devolvidas marcadas, para o serviço
 * distinguir envio do CRM de resposta manual; grupo e status/broadcast são ignorados.
 */
@Component
public class EvolutionWebhookParser implements WhatsAppWebhookParser {

    private static final String UPSERT = "messages.upsert";
    private static final String UPDATE = "messages.update";

    @Override
    public boolean isInboundMessage(Map<String, Object> raw) {
        return UPSERT.equals(event(raw));
    }

    @Override
    public Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw) {
        if (!isInboundMessage(raw)) {
            return Optional.empty();
        }
        Map<?, ?> data = map(raw.get("data"));
        Map<?, ?> key = map(data.get("key"));
        boolean fromMe = Boolean.TRUE.equals(key.get("fromMe"));
        String id = str(key.get("id"));
        String from = senderPhone(key, data);
        if (id == null || from == null) {
            return Optional.empty();
        }
        // Em fromMe o pushName é o do próprio dono do número, não do contato.
        String senderName = fromMe ? null : str(data.get("pushName"));
        return Optional.of(new InboundMessageData(id, from, instance(raw), text(data), senderName, fromMe,
                mediaType(data)));
    }

    @Override
    public boolean isStatusUpdate(Map<String, Object> raw) {
        return UPDATE.equals(event(raw));
    }

    @Override
    public Optional<StatusData> parseStatusUpdate(Map<String, Object> raw) {
        if (!isStatusUpdate(raw)) {
            return Optional.empty();
        }
        Map<?, ?> data = map(raw.get("data"));
        String id = str(data.get("keyId"));
        MessageStatus status = mapStatus(str(data.get("status")));
        if (id == null || status == null) {
            return Optional.empty();
        }
        return Optional.of(new StatusData(id, status,
                status == MessageStatus.FAILED ? "Evolution reportou ERROR" : null));
    }

    @Override
    public String providerChannelReference(Map<String, Object> raw) {
        return instance(raw);
    }

    @Override
    public String providerName() {
        return "EVOLUTION";
    }

    /** {@code MESSAGES_UPSERT} e {@code messages.upsert} são equivalentes. */
    private static String event(Map<String, Object> raw) {
        String e = str(raw.get("event"));
        return e == null ? null : e.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    private static String instance(Map<String, Object> raw) {
        return str(raw.get("instance"));
    }

    /**
     * Telefone (só dígitos) do remetente. Contatos endereçados por LID trazem o
     * número real em {@code remoteJidAlt}/{@code senderPn}. Grupos e broadcast → null.
     */
    private static String senderPhone(Map<?, ?> key, Map<?, ?> data) {
        String jid = str(key.get("remoteJid"));
        if (jid == null || jid.endsWith("@g.us") || jid.endsWith("@broadcast")) {
            return null;
        }
        if (jid.endsWith("@lid")) {
            jid = firstNonNull(str(key.get("remoteJidAlt")), str(key.get("senderPn")), str(data.get("senderPn")));
            if (jid == null) {
                return null;
            }
        }
        String digits = jid.substring(0, jid.contains("@") ? jid.indexOf('@') : jid.length())
                .replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private static String text(Map<?, ?> data) {
        Map<?, ?> message = map(data.get("message"));
        String body = firstNonNull(
                str(message.get("conversation")),
                str(map(message.get("extendedTextMessage")).get("text")),
                str(map(message.get("imageMessage")).get("caption")),
                str(map(message.get("videoMessage")).get("caption")),
                str(map(message.get("documentMessage")).get("caption")));
        if (body != null) {
            return body;
        }
        String type = str(data.get("messageType"));
        return "[" + (type != null ? type : "mensagem") + "]";
    }

    /** Áudio, foto e PDF são entendidos pelo agente; demais mídias ficam como texto/placeholder. */
    private static MessageType mediaType(Map<?, ?> data) {
        Map<?, ?> message = map(data.get("message"));
        if (!map(message.get("audioMessage")).isEmpty()) {
            return MessageType.AUDIO;
        }
        if (!map(message.get("imageMessage")).isEmpty()) {
            return MessageType.IMAGE;
        }
        Map<?, ?> document = map(message.get("documentMessage"));
        if (document.isEmpty()) {
            document = map(map(map(message.get("documentWithCaptionMessage")).get("message")).get("documentMessage"));
        }
        String mime = str(document.get("mimetype"));
        if (mime != null && mime.toLowerCase(Locale.ROOT).contains("pdf")) {
            return MessageType.DOCUMENT;
        }
        return MessageType.TEXT;
    }

    private static MessageStatus mapStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "SERVER_ACK" -> MessageStatus.SENT;
            case "DELIVERY_ACK" -> MessageStatus.DELIVERED;
            case "READ", "PLAYED" -> MessageStatus.READ;
            case "ERROR" -> MessageStatus.FAILED;
            default -> null;
        };
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
