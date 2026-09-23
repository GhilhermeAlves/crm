package com.becommerce.crm.presentation.rest.identity;

import com.becommerce.crm.application.identity.port.input.AuthUseCase;
import com.becommerce.crm.application.identity.port.input.UserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final AuthUseCase authUseCase = mock(AuthUseCase.class);
    private final UserUseCase userUseCase = mock(UserUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authUseCase, userUseCase))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void forgotPassword_comEmailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-e-email\"}"))
                .andExpect(status().isBadRequest());
        verify(authUseCase, never()).forgotPassword(any());
    }

    @Test
    void forgotPassword_comEmailValido_chamaUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usuario@empresa.com\"}"))
                .andExpect(status().isAccepted());
        verify(authUseCase).forgotPassword("usuario@empresa.com");
    }

    @Test
    void resetPassword_senhaCurta_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok-123\",\"newPassword\":\"curta\"}"))
                .andExpect(status().isBadRequest());
        verify(authUseCase, never()).resetPassword(any(), any());
    }

    @Test
    void changePassword_novaSenhaCurta_retorna400() throws Exception {
        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Atual123!\",\"newPassword\":\"curta\"}"))
                .andExpect(status().isBadRequest());
        verify(authUseCase, never()).changePassword(any(), any(), any());
    }
}