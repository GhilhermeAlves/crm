package com.becommerce.crm.communication.telegram.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cliente da Bot API do Telegram. Nunca inclua o token em mensagens de erro ou logs. */
@Component
public class TelegramBotClient {

    static final int MAX_LEN = 4096;

    private final RestClient rest;
    private final String baseUrl;

    public TelegramBotClient(RestClient.Builder builder,
                             @Value("${app.telegram.api-base-url:https://api.telegram.org}") String baseUrl) {
        this.rest = builder.build();
        this.baseUrl = baseUrl;
    }

    public record Button(String text, String callbackData) {}

    public static class TelegramApiException extends RuntimeException {
        public TelegramApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public String getMe(String token) {
        JsonNode r = call(token, "getMe", null);
        return r.path("username").asText();
    }

    public void setWebhook(String token, String url, String secret) {
        call(token, "setWebhook", Map.of("url", url, "secret_token", secret,
                "allowed_updates", List.of("message", "callback_query")));
    }

    public void deleteWebhook(String token) {
        call(token, "deleteWebhook", Map.of());
    }

    public long sendMessage(String token, long chatId, String text, List<List<Button>> keyboard) {
        List<String> parts = split(text);
        long last = 0;
        for (int i = 0; i < parts.size(); i++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("chat_id", chatId);
            body.put("text", parts.get(i));
            if (keyboard != null && i == parts.size() - 1) {
                body.put("reply_markup", markup(keyboard));
            }
            last = call(token, "sendMessage", body).path("message_id").asLong();
        }
        return last;
    }

    public void editMessage(String token, long chatId, long messageId, String text) {
        call(token, "editMessageText", Map.of("chat_id", chatId, "message_id", messageId,
                "text", text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text));
    }

    public void answerCallback(String token, String callbackId) {
        call(token, "answerCallbackQuery", Map.of("callback_query_id", callbackId));
    }

    public byte[] downloadFile(String token, String fileId) {
        try {
            JsonNode r = rest.get().uri(baseUrl + "/bot" + token + "/getFile?file_id={fileId}", fileId)
                    .retrieve().body(JsonNode.class);
            String path = r == null ? "" : r.path("result").path("file_path").asText();
            if (path.isBlank()) {
                throw new TelegramApiException("Telegram não retornou o caminho do arquivo", null);
            }
            return rest.get().uri(baseUrl + "/file/bot" + token + "/" + path).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            throw new TelegramApiException("Falha ao baixar arquivo do Telegram" + statusOf(e), null);
        }
    }

    private JsonNode call(String token, String method, Object body) {
        try {
            JsonNode r;
            if (body == null) {
                r = rest.get().uri(baseUrl + "/bot" + token + "/" + method)
                        .retrieve().body(JsonNode.class);
            } else {
                r = rest.post().uri(baseUrl + "/bot" + token + "/" + method)
                        .contentType(MediaType.APPLICATION_JSON).body(body)
                        .retrieve().body(JsonNode.class);
            }
            if (r == null || !r.path("ok").asBoolean()) {
                throw new TelegramApiException("Telegram recusou " + method, null);
            }
            return r.path("result");
        } catch (RestClientException e) {
            // Sem causa: a mensagem do RestClientException contém a URL, que carrega o token.
            throw new TelegramApiException("Falha ao chamar Telegram: " + method + statusOf(e), null);
        }
    }

    private static String statusOf(RestClientException e) {
        return e instanceof RestClientResponseException re ? " (HTTP " + re.getStatusCode().value() + ")" : "";
    }

    private static Map<String, Object> markup(List<List<Button>> keyboard) {
        return Map.of("inline_keyboard", keyboard.stream().map(row -> row.stream()
                .map(b -> Map.of("text", b.text(), "callback_data", b.callbackData())).toList()).toList());
    }

    static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < text.length(); i += MAX_LEN) {
            parts.add(text.substring(i, Math.min(text.length(), i + MAX_LEN)));
        }
        if (parts.isEmpty()) parts.add(text);
        return parts;
    }
}
