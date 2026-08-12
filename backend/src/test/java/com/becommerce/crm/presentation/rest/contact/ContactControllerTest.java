package com.becommerce.crm.presentation.rest.contact;

import com.becommerce.crm.application.contact.dto.ContactResponse;
import com.becommerce.crm.application.contact.dto.CreateContactRequest;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
import com.becommerce.crm.domain.quota.exception.QuotaExceededException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.presentation.rest.handler.GlobalExceptionHandler;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock private ContactUseCase contactUseCase;
    @InjectMocks private ContactController contactController;

    private MockMvc mockMvc;
    private UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contactController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(UUID activeCompanyId) {
        CurrentUser principal = new CurrentUser(
                UUID.randomUUID(), "admin@empresa.com", activeCompanyId, activeCompanyId,
                List.of("ADMIN"), List.of("contact:create", "contact:read"),
                "k", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void shouldCreateContactInOwnCompany() throws Exception {
        login(companyId);
        when(contactUseCase.create(any(), any(CreateContactRequest.class), any(UUID.class)))
                .thenReturn(new ContactResponse(UUID.randomUUID(), companyId, "Ana", "Souza", "ana@e.com", null, null,
                        java.time.LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ana\",\"lastName\":\"Souza\",\"email\":\"ana@e.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ana"));
    }

    @Test
    void shouldReturn403WhenCreatingContactForOtherCompany() throws Exception {
        UUID otherCompany = UUID.randomUUID();
        login(companyId);

        mockMvc.perform(post("/api/v1/companies/" + otherCompany + "/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ana\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }

    @Test
    void shouldReturn422WhenQuotaReached() throws Exception {
        login(companyId);
        when(contactUseCase.create(any(), any(CreateContactRequest.class), any(UUID.class)))
                .thenThrow(new QuotaExceededException("Limite de contatos da empresa atingido (500)."));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ana\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"));
    }
}