package com.becommerce.crm.masterdata.contact.registration.web;

import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationLinkResponse;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationPreviewResponse;
import com.becommerce.crm.masterdata.contact.registration.application.service.RegistrationLinkService;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNoLongerValidException;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNotFoundException;
import com.becommerce.crm.masterdata.contact.registration.infrastructure.rate.RegistrationRateLimiter;
import com.becommerce.crm.shared.security.config.CurrentCompanyIdArgumentResolver;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegistrationLinkControllerTest {

    @Mock private RegistrationLinkService service;
    @Mock private RegistrationRateLimiter rateLimiter;
    @InjectMocks private RegistrationLinkController controller;

    private MockMvc mockMvc;
    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    private static final String BODY = """
            {"firstName":"Maria","lastName":"Silva","cpf":"529.982.247-25","birthDate":"1990-05-01",
             "email":"maria@ex.com","phone":"11988887777",
             "address":{"zipCode":"01310100","street":"Av. Paulista","number":"1000","state":"SP"},
             "website":""}""";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentCompanyIdArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
        CurrentUser principal = new CurrentUser(userId, "admin@empresa.com", companyId, companyId,
                List.of("ADMIN"), List.of("contact:update"), "keycloak-sub", null, "keycloak", "Admin", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_returns201WithUrl() throws Exception {
        when(service.create(companyId, contactId, userId)).thenReturn(
                new RegistrationLinkResponse("https://app/cadastro/abc", LocalDateTime.of(2026, 10, 30, 12, 0)));

        mockMvc.perform(post("/api/v1/companies/{c}/patients/{p}/registration-link", companyId, contactId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://app/cadastro/abc"));
    }

    @Test
    void preview_ok() throws Exception {
        when(rateLimiter.tryAcquire("10.0.0.1")).thenReturn(true);
        when(service.preview("tok")).thenReturn(new RegistrationPreviewResponse("Clínica", "Maria", "Silva", "119"));

        mockMvc.perform(get("/api/v1/public/registration/tok").header("X-Real-IP", "10.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Clínica"));
    }

    @Test
    void preview_unknown404_and_used410() throws Exception {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(service.preview("x")).thenThrow(new RegistrationLinkNotFoundException());
        when(service.preview("y")).thenThrow(new RegistrationLinkNoLongerValidException("expirou"));

        mockMvc.perform(get("/api/v1/public/registration/x")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/registration/y")).andExpect(status().isGone());
    }

    @Test
    void submit_returns204() throws Exception {
        when(rateLimiter.tryAcquire("unknown")).thenReturn(true);

        mockMvc.perform(post("/api/v1/public/registration/tok")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent());
        verify(service).submit(eq("tok"), any());
    }

    @Test
    void submit_missingFirstName_400() throws Exception {
        mockMvc.perform(post("/api/v1/public/registration/tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lastName\":\"S\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).submit(anyString(), any());
    }

    @Test
    void submit_fieldsAboveColumnLimits_400() throws Exception {
        String[] bodies = {
                "{\"firstName\":\"" + "M".repeat(101) + "\"}",
                "{\"firstName\":\"M\",\"email\":\"" + "a".repeat(250) + "@ex.com\"}",
                "{\"firstName\":\"M\",\"cpf\":\"" + "1".repeat(15) + "\"}",
                "{\"firstName\":\"M\",\"address\":{\"city\":\"" + "c".repeat(81) + "\"}}",
                "{\"firstName\":\"M\",\"address\":{\"state\":\"SPX\"}}",
                "{\"firstName\":\"M\",\"address\":{\"zipCode\":\"" + "1".repeat(11) + "\"}}"};
        for (String body : bodies) {
            mockMvc.perform(post("/api/v1/public/registration/tok")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(service, never()).submit(anyString(), any());
    }

    @Test
    void rateLimited_429() throws Exception {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/v1/public/registration/tok")).andExpect(status().isTooManyRequests());
        verify(service, never()).preview(anyString());
    }
}
