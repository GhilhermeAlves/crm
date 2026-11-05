package com.becommerce.crm.communication.telegram.web;

import com.becommerce.crm.communication.telegram.application.TelegramConfigService;
import com.becommerce.crm.communication.telegram.application.TelegramUpdateDispatcher;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramUpdateSeenJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TelegramWebhookControllerTest {

    static final UUID CO = UUID.randomUUID();

    @Mock TelegramConfigService config;
    @Mock TelegramUpdateSeenJpaRepository seen;
    @Mock TelegramUpdateDispatcher dispatcher;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new TelegramWebhookController(config, seen, dispatcher, new ObjectMapper())).build();
    }

    @Test
    void missingOrWrongSecretIs401AndNothingProcessed() throws Exception {
        when(config.tokenIfValidSecret(CO, null)).thenReturn(Optional.empty());
        mvc.perform(post("/api/v1/telegram/webhook/" + CO).contentType(APPLICATION_JSON).content("{\"update_id\":1}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(dispatcher, seen);
    }

    @Test
    void validNewUpdateIsDispatched() throws Exception {
        when(config.tokenIfValidSecret(CO, "S")).thenReturn(Optional.of("TOK"));
        when(seen.markSeen(CO, 1L)).thenReturn(1);
        mvc.perform(post("/api/v1/telegram/webhook/" + CO).header("X-Telegram-Bot-Api-Secret-Token", "S")
                        .contentType(APPLICATION_JSON).content("{\"update_id\":1,\"message\":{\"text\":\"oi\"}}"))
                .andExpect(status().isOk());
        verify(dispatcher).dispatch(eq(CO), eq("TOK"), argThat(n -> n.path("update_id").asLong() == 1));
    }

    @Test
    void repeatedUpdateIsIgnored() throws Exception {
        when(config.tokenIfValidSecret(CO, "S")).thenReturn(Optional.of("TOK"));
        when(seen.markSeen(CO, 1L)).thenReturn(0);
        mvc.perform(post("/api/v1/telegram/webhook/" + CO).header("X-Telegram-Bot-Api-Secret-Token", "S")
                        .contentType(APPLICATION_JSON).content("{\"update_id\":1}"))
                .andExpect(status().isOk());
        verifyNoInteractions(dispatcher);
    }
}
