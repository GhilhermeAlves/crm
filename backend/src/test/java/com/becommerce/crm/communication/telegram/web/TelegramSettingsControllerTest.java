package com.becommerce.crm.communication.telegram.web;

import com.becommerce.crm.communication.telegram.application.TelegramConfigService;
import com.becommerce.crm.communication.telegram.application.TelegramConfigService.TelegramConfigResponse;
import com.becommerce.crm.communication.telegram.application.TelegramLinkService;
import com.becommerce.crm.shared.security.config.CurrentCompanyIdArgumentResolver;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TelegramSettingsControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final String URL = "/api/v1/companies/" + COMPANY_ID + "/telegram";

    @Mock private TelegramConfigService config;
    @Mock private TelegramLinkService links;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TelegramSettingsController(config, links))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentCompanyIdArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
        CurrentUser principal = new CurrentUser(
                USER_ID, "admin@empresa.com", COMPANY_ID, COMPANY_ID,
                List.of("ADMIN"), List.of("company:update"),
                "keycloak-sub", null, "keycloak", "Admin", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("company:update"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void putSavesTokenAndReturnsBotUsername() throws Exception {
        when(config.save(COMPANY_ID, "TOK", USER_ID))
                .thenReturn(new TelegramConfigResponse(true, "leo_bot", true, LocalDateTime.now()));

        mockMvc.perform(put(URL).contentType("application/json").content("{\"token\":\"TOK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botUsername").value("leo_bot"))
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    void putWithBlankTokenReturns400() throws Exception {
        mockMvc.perform(put(URL).contentType("application/json").content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(config, never()).save(any(), anyString(), any());
    }

    @Test
    void getNeverExposesToken() throws Exception {
        when(config.get(COMPANY_ID))
                .thenReturn(new TelegramConfigResponse(true, "leo_bot", true, LocalDateTime.now()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botUsername").value("leo_bot"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void linkCodeReturnsCodeAndBotUsername() throws Exception {
        when(config.get(COMPANY_ID))
                .thenReturn(new TelegramConfigResponse(true, "leo_bot", true, LocalDateTime.now()));
        when(links.generateCode(COMPANY_ID, USER_ID)).thenReturn("123456");

        mockMvc.perform(post(URL + "/link-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("123456"))
                .andExpect(jsonPath("$.botUsername").value("leo_bot"))
                .andExpect(jsonPath("$.expiresInMinutes").value(10));
    }

    @Test
    void linkCodeWithoutConfiguredBotReturns4xx() throws Exception {
        when(config.get(COMPANY_ID))
                .thenReturn(new TelegramConfigResponse(false, null, false, null));

        mockMvc.perform(post(URL + "/link-code"))
                .andExpect(status().is4xxClientError());
        verify(links, never()).generateCode(any(), any());
    }

    @Test
    void deleteMeUnlinksCurrentUser() throws Exception {
        mockMvc.perform(delete(URL + "/me"))
                .andExpect(status().isNoContent());
        verify(links).unlinkUser(COMPANY_ID, USER_ID);
    }
}
