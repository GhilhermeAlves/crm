package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.dto.AiChatRequest;
import com.becommerce.crm.automation.ai.application.dto.AiChatResponse;
import com.becommerce.crm.automation.ai.application.port.input.AiActionUseCase;
import com.becommerce.crm.automation.ai.application.port.input.AiAssistantUseCase;
import com.becommerce.crm.automation.ai.application.port.output.AiMediaProvider;
import com.becommerce.crm.automation.ai.domain.AiActionNotFoundException;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramBotClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramUpdateProcessorTest {

    static final UUID CO = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID CONV = UUID.randomUUID();
    static final UUID ACTION = UUID.randomUUID();
    static final long TG = 555L;

    @Mock TelegramBotClient bot;
    @Mock TelegramLinkService links;
    @Mock TelegramUserResolver users;
    @Mock AiAssistantUseCase leo;
    @Mock AiActionUseCase actions;
    @Mock AiMediaProvider media;
    @Mock TelegramRateLimiter limiter;
    @Mock TenantAuditRecorder auditor;
    final ObjectMapper om = new ObjectMapper();
    TelegramUpdateProcessor p;

    @BeforeEach
    void setUp() {
        p = new TelegramUpdateProcessor(bot, links, users, leo, actions, media, limiter, auditor);
        lenient().when(limiter.tryAcquire(any(), anyLong())).thenReturn(true);
    }

    JsonNode msg(String json) throws Exception {
        return om.readTree("{\"update_id\":1,\"message\":{\"chat\":{\"id\":555,\"type\":\"private\"},\"from\":{\"id\":555,\"first_name\":\"Gui\"},"
                + json + "}}");
    }

    TelegramUserResolver.LinkedUser linked(List<String> perms) {
        return new TelegramUserResolver.LinkedUser(USER, TG, CONV, perms, List.of());
    }

    @Test
    void textReachesLeoUnchanged() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        when(leo.chat(eq(CO), eq(USER), eq(List.of("ai:chat")), any()))
                .thenReturn(new AiChatResponse(CONV, "ok", "openai"));
        p.process(CO, "TOK", msg("\"text\":\"  marca a Maria dia 13 às 14h \""));
        ArgumentCaptor<AiChatRequest> req = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(leo).chat(eq(CO), eq(USER), any(), req.capture());
        assertEquals("  marca a Maria dia 13 às 14h ", req.getValue().message());
        assertEquals(CONV, req.getValue().conversationId());
        verify(bot).sendMessage("TOK", TG, "ok", null);
    }

    @Test
    void unlinkedSenderNeverReachesLeo() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.empty());
        p.process(CO, "TOK", msg("\"text\":\"oi\""));
        verifyNoInteractions(leo);
        verify(bot).sendMessage(eq("TOK"), eq(TG), contains("Conta não vinculada"), isNull());
    }

    @Test
    void linkCommandCallsService() throws Exception {
        when(links.link(CO, "123456", TG, TG, "Gui")).thenReturn(TelegramLinkService.LinkResult.LINKED);
        p.process(CO, "TOK", msg("\"text\":\"/vincular 123456\""));
        verify(bot).sendMessage(eq("TOK"), eq(TG), contains("vinculado"), isNull());
        verifyNoInteractions(leo);
    }

    @Test
    void voiceIsTranscribedPassedUnchangedAndEchoed() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        when(bot.downloadFile("TOK", "F1")).thenReturn(new byte[]{1});
        when(media.transcribe(new byte[]{1}, "audio/ogg")).thenReturn("marca a Maria");
        when(leo.chat(any(), any(), any(), any())).thenReturn(new AiChatResponse(CONV, "Certo.", "openai"));
        p.process(CO, "TOK", msg("\"voice\":{\"file_id\":\"F1\",\"mime_type\":\"audio/ogg\"}"));
        ArgumentCaptor<AiChatRequest> req = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(leo).chat(any(), any(), any(), req.capture());
        assertEquals("marca a Maria", req.getValue().message());
        verify(bot).sendMessage("TOK", TG, "🎙️ Entendi: \"marca a Maria\"\n\nCerto.", null);
    }

    @Test
    void proposedActionGetsConfirmButtons() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        AiActionResponse a = new AiActionResponse(ACTION, CONV, "cancel_appointment", null, null,
                "Cancelar consulta de Maria 13/11 14h", "PROPOSED", Map.of(), null, null, null, null);
        when(leo.chat(any(), any(), any(), any()))
                .thenReturn(new AiChatResponse(CONV, "Confirma?", "openai", List.of(a)));
        p.process(CO, "TOK", msg("\"text\":\"cancela a Maria\""));
        verify(bot).sendMessage("TOK", TG, "Cancelar consulta de Maria 13/11 14h",
                List.of(List.of(new TelegramBotClient.Button("✅ Confirmar", "c:" + ACTION),
                        new TelegramBotClient.Button("❌ Cancelar", "x:" + ACTION))));
    }

    @Test
    void confirmCallbackUsesClickerIdentity() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat", "appointment:update"))));
        AiActionResponse done = new AiActionResponse(ACTION, CONV, "t", null, null, "Cancelar consulta", "EXECUTED",
                Map.of(), null, null, null, null);
        when(actions.confirm(CO, USER, List.of("ai:chat", "appointment:update"), ACTION)).thenReturn(done);
        JsonNode cb = om.readTree("{\"update_id\":2,\"callback_query\":{\"id\":\"cb1\",\"from\":{\"id\":555},\"data\":\"c:"
                + ACTION + "\",\"message\":{\"message_id\":9,\"chat\":{\"id\":555,\"type\":\"private\"}}}}");
        p.process(CO, "TOK", cb);
        verify(bot).answerCallback("TOK", "cb1");
        verify(bot).editMessage("TOK", 555L, 9L, "✅ Feito: Cancelar consulta");
        verify(auditor).record(eq(CO), any(), any(), eq("AiAction"), eq(ACTION.toString()),
                contains("via Telegram"), eq(USER), argThat(m -> "telegram".equals(m.get("channel"))));
    }

    @Test
    void callbackFromOtherUsersActionIsRejectedByUseCase() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        when(actions.confirm(any(), any(), any(), any()))
                .thenThrow(new AiActionNotFoundException("Acao nao encontrada ou sem acesso."));
        JsonNode cb = om.readTree("{\"update_id\":2,\"callback_query\":{\"id\":\"cb1\",\"from\":{\"id\":555},\"data\":\"c:"
                + ACTION + "\",\"message\":{\"message_id\":9,\"chat\":{\"id\":555,\"type\":\"private\"}}}}");
        p.process(CO, "TOK", cb);
        verify(bot).editMessage(eq("TOK"), eq(555L), eq(9L), startsWith("⚠️"));
    }

    @Test
    void withoutAiChatPermissionIsBlocked() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of())));
        p.process(CO, "TOK", msg("\"text\":\"oi\""));
        verifyNoInteractions(leo);
    }

    @Test
    void groupMessagesAreIgnored() throws Exception {
        JsonNode g = om.readTree("{\"update_id\":3,\"message\":{\"chat\":{\"id\":-1,\"type\":\"group\"},\"from\":{\"id\":555},\"text\":\"oi\"}}");
        p.process(CO, "TOK", g);
        verifyNoInteractions(bot, leo, users);
    }

    @Test
    void rateLimitedSenderGetsWarning() throws Exception {
        when(limiter.tryAcquire(CO, TG)).thenReturn(false);
        p.process(CO, "TOK", msg("\"text\":\"oi\""));
        verifyNoInteractions(leo);
        verify(bot).sendMessage(eq("TOK"), eq(TG), contains("Muitas mensagens"), isNull());
    }

    @Test
    void cancelCallbackAuditsAndEdits() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        JsonNode cb = om.readTree("{\"update_id\":2,\"callback_query\":{\"id\":\"cb1\",\"from\":{\"id\":555},\"data\":\"x:"
                + ACTION + "\",\"message\":{\"message_id\":9,\"chat\":{\"id\":555,\"type\":\"private\"}}}}");
        p.process(CO, "TOK", cb);
        verify(actions).cancel(CO, USER, ACTION);
        verify(bot).editMessage("TOK", 555L, 9L, "❌ Cancelado.");
        verify(auditor).record(eq(CO), any(), any(), eq("AiAction"), eq(ACTION.toString()),
                contains("via Telegram"), eq(USER),
                argThat(m -> "AI_ACTION_CANCELLED_VIA_TELEGRAM".equals(m.get("event"))));
    }

    @Test
    void transcriptionFailureAsksToRepeat() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        when(bot.downloadFile("TOK", "F1")).thenReturn(new byte[]{1});
        when(media.transcribe(any(), any())).thenThrow(new RuntimeException("boom"));
        p.process(CO, "TOK", msg("\"voice\":{\"file_id\":\"F1\",\"mime_type\":\"audio/ogg\"}"));
        verifyNoInteractions(leo);
        verify(bot).sendMessage(eq("TOK"), eq(TG), contains("Não entendi o áudio"), isNull());
    }

    @Test
    void crmAdminWithoutAiChatPermissionReachesLeo() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(
                new TelegramUserResolver.LinkedUser(USER, TG, CONV, List.of(), List.of("CRM_ADMIN"))));
        when(leo.chat(any(), any(), any(), any())).thenReturn(new AiChatResponse(CONV, "ok", "openai"));
        p.process(CO, "TOK", msg("\"text\":\"oi\""));
        verify(leo).chat(any(), any(), any(), any());
    }

    @Test
    void unexpectedCallbackErrorIsNotEchoed() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        when(actions.confirm(any(), any(), any(), any())).thenThrow(new RuntimeException("jdbc secret"));
        JsonNode cb = om.readTree("{\"update_id\":2,\"callback_query\":{\"id\":\"cb1\",\"from\":{\"id\":555},\"data\":\"c:"
                + ACTION + "\",\"message\":{\"message_id\":9,\"chat\":{\"id\":555,\"type\":\"private\"}}}}");
        p.process(CO, "TOK", cb);
        verify(bot).editMessage("TOK", 555L, 9L, "⚠️ Não foi possível: erro inesperado");
    }
}
