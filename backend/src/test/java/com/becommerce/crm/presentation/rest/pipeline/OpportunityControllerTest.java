package com.becommerce.crm.presentation.rest.pipeline;

import com.becommerce.crm.application.pipeline.dto.OpportunityResponse;
import com.becommerce.crm.application.pipeline.port.input.OpportunityUseCase;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OpportunityControllerTest {

    @Mock private OpportunityUseCase opportunityUseCase;
    @InjectMocks private OpportunityController opportunityController;

    private MockMvc mockMvc;
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(opportunityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login() {
        CurrentUser principal = new CurrentUser(
                UUID.randomUUID(), "admin@empresa.com", companyId, companyId,
                List.of("ADMIN"),
                List.of("opportunity:create", "opportunity:read", "opportunity:update",
                        "opportunity:delete", "opportunity:move", "opportunity:win", "opportunity:lose"),
                "k", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private OpportunityResponse response() {
        UUID stageId = UUID.randomUUID();
        return new OpportunityResponse(UUID.randomUUID(), companyId, "Oportunidade A",
                new BigDecimal("150.00"), UUID.randomUUID(), UUID.randomUUID(), stageId,
                "Qualificação", 45, null, null, OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldCreateOpportunity() throws Exception {
        login();
        UUID pipelineId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        when(opportunityUseCase.create(eq(companyId), eq(pipelineId), any(), any(UUID.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/pipelines/" + pipelineId + "/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Oportunidade A\",\"value\":150.00,\"contactId\":\""
                                + contactId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Oportunidade A"));
    }

    @Test
    void shouldReturn400WhenCreatingOpportunityWithoutValue() throws Exception {
        login();
        UUID pipelineId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/pipelines/" + pipelineId + "/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Sem valor\",\"contactId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListByPipeline() throws Exception {
        login();
        UUID pipelineId = UUID.randomUUID();
        when(opportunityUseCase.listByPipeline(companyId, pipelineId)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/pipelines/" + pipelineId + "/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageName").value("Qualificação"));
    }

    @Test
    void shouldMoveOpportunity() throws Exception {
        login();
        UUID opportunityId = UUID.randomUUID();
        when(opportunityUseCase.move(eq(companyId), eq(opportunityId), any(), any(UUID.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/opportunities/" + opportunityId + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"ADVANCE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenMovingWithoutDirection() throws Exception {
        login();
        UUID opportunityId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/opportunities/" + opportunityId + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMarkWon() throws Exception {
        login();
        UUID opportunityId = UUID.randomUUID();
        when(opportunityUseCase.markWon(eq(companyId), eq(opportunityId), any(UUID.class))).thenReturn(response());

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/opportunities/" + opportunityId + "/won"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldMarkLost() throws Exception {
        login();
        UUID opportunityId = UUID.randomUUID();
        when(opportunityUseCase.markLost(eq(companyId), eq(opportunityId), any(), any(UUID.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/opportunities/" + opportunityId + "/lost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lossReason\":\"Preço alto\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenMarkingLostWithoutReason() throws Exception {
        login();
        UUID opportunityId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/opportunities/" + opportunityId + "/lost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteOpportunity() throws Exception {
        login();
        UUID opportunityId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/companies/" + companyId + "/opportunities/" + opportunityId))
                .andExpect(status().isNoContent());
        verify(opportunityUseCase).delete(companyId, opportunityId);
    }

    @Test
    void shouldReturn403WhenCreatingForOtherCompany() throws Exception {
        login();
        mockMvc.perform(post("/api/v1/companies/" + UUID.randomUUID() + "/pipelines/" + UUID.randomUUID()
                                + "/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"value\":10,\"contactId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}
