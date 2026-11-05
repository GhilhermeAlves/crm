package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.dto.AiChatRequest;
import com.becommerce.crm.automation.ai.application.dto.AiChatResponse;
import com.becommerce.crm.automation.ai.application.port.input.AiActionUseCase;
import com.becommerce.crm.automation.ai.application.port.input.AiAssistantUseCase;
import com.becommerce.crm.automation.ai.application.port.output.AiMediaProvider;
import com.becommerce.crm.automation.ai.domain.AiActionInvalidStateException;
import com.becommerce.crm.automation.ai.domain.AiActionNotFoundException;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramBotClient;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelegramUpdateProcessor {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateProcessor.class);
    static final String NOT_LINKED = "Conta não vinculada. Gere o código no seu perfil do CRM (Perfil → Conectar Telegram) e envie /vincular CÓDIGO.";
    private static final String AUDIO_FAIL = "Não entendi o áudio, pode repetir ou escrever?";

    private final TelegramBotClient bot;
    private final TelegramLinkService links;
    private final TelegramUserResolver users;
    private final AiAssistantUseCase leo;
    private final AiActionUseCase actions;
    private final AiMediaProvider media;
    private final TelegramRateLimiter limiter;
    private final TenantAuditRecorder auditor;
    private final TelegramActivityNotifier notifier;

    public TelegramUpdateProcessor(TelegramBotClient bot, TelegramLinkService links, TelegramUserResolver users,
                                   AiAssistantUseCase leo, AiActionUseCase actions, AiMediaProvider media,
                                   TelegramRateLimiter limiter, TenantAuditRecorder auditor,
                                   TelegramActivityNotifier notifier) {
        this.bot = bot;
        this.links = links;
        this.users = users;
        this.leo = leo;
        this.actions = actions;
        this.media = media;
        this.limiter = limiter;
        this.auditor = auditor;
        this.notifier = notifier;
    }

    public void process(UUID companyId, String token, JsonNode update) {
        if (update.has("callback_query")) {
            handleCallback(companyId, token, update.get("callback_query"));
            return;
        }
        JsonNode m = update.path("message");
        if (m.isMissingNode() || !"private".equals(m.path("chat").path("type").asText())) {
            return;
        }
        long chatId = m.path("chat").path("id").asLong();
        long from = m.path("from").path("id").asLong();
        if (!limiter.tryAcquire(companyId, from)) {
            send(token, chatId, "Muitas mensagens em pouco tempo. Aguarde um minuto.");
            return;
        }

        String text = m.has("text") ? m.get("text").asText() : null;
        if (text != null && text.startsWith("/")) {
            if (handleCommand(companyId, token, chatId, from, m, text)) {
                return;
            }
        }

        Optional<TelegramUserResolver.LinkedUser> user = users.resolve(companyId, from);
        if (user.isEmpty()) {
            send(token, chatId, NOT_LINKED);
            return;
        }
        if (!user.get().permissions().contains("ai:chat")
                && !user.get().roles().contains(CurrentUser.CRM_ADMIN_ROLE)) {
            send(token, chatId, "Seu usuário não tem acesso ao assistente.");
            return;
        }

        String prefix = "";
        if (text == null) {
            JsonNode voice = m.has("voice") ? m.get("voice") : m.get("audio");
            if (voice == null) {
                send(token, chatId, "Por enquanto entendo texto e áudio.");
                return;
            }
            try {
                byte[] audio = bot.downloadFile(token, voice.path("file_id").asText());
                text = media.transcribe(audio, voice.path("mime_type").asText("audio/ogg"));
            } catch (RuntimeException e) {
                log.warn("[TELEGRAM] falha na transcrição (company={})", companyId, e);
                send(token, chatId, AUDIO_FAIL);
                return;
            }
            if (text == null || text.isBlank()) {
                send(token, chatId, AUDIO_FAIL);
                return;
            }
            prefix = "🎙️ Entendi: \"" + text + "\"\n\n";
        }
        askLeo(companyId, token, chatId, from, user.get(), text, prefix);
    }

    /** true se o comando foi tratado aqui; false se deve seguir para o Leo. */
    private boolean handleCommand(UUID companyId, String token, long chatId, long from, JsonNode m, String text) {
        String[] parts = text.trim().split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT).replaceAll("@.*$", "");
        switch (cmd) {
            case "/start" -> send(token, chatId,
                    "Olá! Sou o Leo, assistente do CRM. Para começar, gere o código no seu perfil do CRM e envie /vincular CÓDIGO.");
            case "/vincular" -> {
                String name = m.path("from").path("first_name").asText(null);
                var r = links.link(companyId, parts.length > 1 ? parts[1] : null, from, chatId, name);
                send(token, chatId, switch (r) {
                    case LINKED -> "✅ Telegram vinculado. Pode falar comigo.";
                    case INVALID_CODE -> "Código inválido ou vencido. Gere outro no seu perfil do CRM.";
                    case TELEGRAM_ALREADY_LINKED -> "Este Telegram já está vinculado a outro usuário.";
                });
            }
            case "/desvincular" -> {
                links.unlinkByTelegram(companyId, from);
                send(token, chatId, "Telegram desvinculado.");
            }
            case "/nova" -> {
                if (users.resolve(companyId, from).isEmpty()) {
                    send(token, chatId, NOT_LINKED);
                } else {
                    links.setConversation(companyId, from, null);
                    send(token, chatId, "Nova conversa iniciada.");
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void askLeo(UUID companyId, String token, long chatId, long from,
                        TelegramUserResolver.LinkedUser user, String text, String prefix) {
        AiChatResponse r;
        try {
            r = leo.chat(companyId, user.userId(), user.permissions(),
                    new AiChatRequest(text, user.conversationId(), null));
        } catch (RuntimeException e) {
            log.error("[TELEGRAM] falha no Leo (company={})", companyId, e);
            send(token, chatId, prefix + "Não consegui processar agora, tente de novo.");
            notifier.messageReceived(companyId, user.userId(), text, !prefix.isEmpty(),
                    "Não consegui processar agora.", 0, user.conversationId());
            return;
        }
        if (r.conversationId() != null && !r.conversationId().equals(user.conversationId())) {
            links.setConversation(companyId, from, r.conversationId());
        }
        send(token, chatId, prefix + (r.message() == null ? "" : r.message()));
        int proposed = 0;
        for (AiActionResponse a : r.actions() == null ? List.<AiActionResponse>of() : r.actions()) {
            if (!"PROPOSED".equals(a.status())) {
                continue;
            }
            proposed++;
            send(token, chatId, a.description(), List.of(List.of(
                    new TelegramBotClient.Button("✅ Confirmar", "c:" + a.id()),
                    new TelegramBotClient.Button("❌ Cancelar", "x:" + a.id()))));
        }
        notifier.messageReceived(companyId, user.userId(), text, !prefix.isEmpty(), r.message(), proposed,
                r.conversationId());
    }

    private void handleCallback(UUID companyId, String token, JsonNode cb) {
        String data = cb.path("data").asText("");
        long from = cb.path("from").path("id").asLong();
        long chatId = cb.path("message").path("chat").path("id").asLong();
        long messageId = cb.path("message").path("message_id").asLong();
        safe(() -> bot.answerCallback(token, cb.path("id").asText()));
        if (data.length() < 3 || data.charAt(1) != ':') {
            return;
        }
        UUID actionId;
        try {
            actionId = UUID.fromString(data.substring(2));
        } catch (IllegalArgumentException e) {
            return;
        }

        Optional<TelegramUserResolver.LinkedUser> user = users.resolve(companyId, from);
        if (user.isEmpty()) {
            send(token, chatId, NOT_LINKED);
            return;
        }
        UUID userId = user.get().userId();
        String result;
        AiActionResponse executed = null;
        try {
            if (data.charAt(0) == 'c') {
                AiActionResponse a = actions.confirm(companyId, userId, user.get().permissions(), actionId);
                if ("EXECUTED".equals(a.status())) {
                    executed = a;
                }
                result = "EXECUTED".equals(a.status()) ? "✅ Feito: " + a.description()
                        : "⚠️ Não foi possível: " + (a.errorMessage() != null ? a.errorMessage() : a.status());
                auditChannel(companyId, userId, actionId, "AI_ACTION_CONFIRMED_VIA_TELEGRAM", a.description(),
                        a.status());
                notifier.actionResult(companyId, userId, actionId, a.status(), a.description(), a.errorMessage());
            } else {
                AiActionResponse cancelled = actions.cancel(companyId, userId, actionId);
                result = "❌ Cancelado.";
                notifier.actionResult(companyId, userId, actionId, "CANCELLED",
                        cancelled != null ? cancelled.description() : "ação", null);
                auditChannel(companyId, userId, actionId, "AI_ACTION_CANCELLED_VIA_TELEGRAM", "Ação cancelada",
                        "CANCELLED");
            }
        } catch (AiActionNotFoundException | AiActionInvalidStateException e) {
            result = "⚠️ Não foi possível: " + (e.getMessage() != null ? e.getMessage() : "erro inesperado");
        } catch (RuntimeException e) {
            log.error("[TELEGRAM] falha ao processar ação {} (company={})", actionId, companyId, e);
            result = "⚠️ Não foi possível: erro inesperado";
        }
        String finalResult = result;
        safe(() -> bot.editMessage(token, chatId, messageId, finalResult));
        if (executed != null) {
            AiActionResponse done = executed;
            safe(() -> continueAfterConfirmation(companyId, token, chatId, from, user.get(), done));
        }
    }

    /**
     * Após uma confirmação EXECUTED, chama o Leo na mesma conversa com uma mensagem
     * de sistema para que ele siga com os passos pendentes do pedido anterior. A
     * resposta e novas propostas chegam ao chat como em uma mensagem normal.
     */
    private void continueAfterConfirmation(UUID companyId, String token, long chatId, long from,
                                           TelegramUserResolver.LinkedUser user, AiActionResponse done) {
        if (!user.permissions().contains("ai:chat") && !user.roles().contains(CurrentUser.CRM_ADMIN_ROLE)) {
            return;
        }
        UUID conversationId = done.conversationId() != null ? done.conversationId() : user.conversationId();
        TelegramUserResolver.LinkedUser inConversation = new TelegramUserResolver.LinkedUser(user.userId(),
                user.chatId(), conversationId, user.permissions(), user.roles());
        askLeo(companyId, token, chatId, from, inConversation, continuationMessage(done), "");
    }

    static String continuationMessage(AiActionResponse done) {
        String entity = done.entityType() != null && done.entityId() != null
                ? " (" + done.entityType() + " " + done.entityId() + ")" : "";
        return "[Sistema] Ação confirmada e executada: " + done.description() + entity + ". "
                + "Continue o pedido anterior do usuário, se ainda houver passos pendentes; "
                + "se não houver, responda apenas 'Tudo certo.'";
    }

    private void auditChannel(UUID companyId, UUID userId, UUID actionId, String event, String description,
                              String status) {
        try {
            auditor.record(companyId, AuditAction.CUSTOM, AuditModule.AI, "AiAction", actionId.toString(),
                    description + " (via Telegram)", userId,
                    Map.of("event", event, "channel", "telegram", "actionId", actionId.toString(),
                            "status", status == null ? "" : status));
        } catch (RuntimeException ignored) {
            // auditoria nunca derruba o fluxo
        }
    }

    private void send(String token, long chatId, String text) {
        send(token, chatId, text, null);
    }

    private void send(String token, long chatId, String text, List<List<TelegramBotClient.Button>> keyboard) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                bot.sendMessage(token, chatId, text, keyboard);
                return;
            } catch (TelegramBotClient.TelegramApiException e) {
                if (attempt == 3) {
                    log.warn("[TELEGRAM] envio falhou após 3 tentativas (chat={})", chatId);
                }
            }
        }
    }

    private static void safe(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException ignored) {
            // melhor esforço
        }
    }
}
