package com.becommerce.crm.presentation.rest.pipeline;

import com.becommerce.crm.application.pipeline.dto.PipelineMetricsResponse;
import com.becommerce.crm.application.pipeline.dto.PipelineResponse;
import com.becommerce.crm.application.pipeline.dto.StageResponse;
import com.becommerce.crm.application.pipeline.port.input.PipelineUseCase;
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
class PipelineControllerTest {

    @Mock private PipelineUseCase pipelineUseCase;
    @InjectMocks private PipelineController pipelineController;

    private MockMvc mockMvc;
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pipelineController)
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
                List.of("ADMIN"), List.of("pipeline:view", "pipeline:update"),
                "k", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private PipelineResponse pipelineResponse() {
        return new PipelineResponse(UUID.randomUUID(), companyId, "Vendas", null, true,
                List.of(new StageResponse(UUID.randomUUID(), UUID.randomUUID(), "Prospecção", null,
                        1, 10, LocalDateTime.now(), LocalDateTime.now())),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldListPipelines() throws Exception {
        login();
        when(pipelineUseCase.list(companyId)).thenReturn(List.of(pipelineResponse()));

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/pipelines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Vendas"));
    }

    @Test
    void shouldReturn403WhenListingPipelinesOfOtherCompany() throws Exception {
        login();
        mockMvc.perform(get("/api/v1/companies/" + UUID.randomUUID() + "/pipelines"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }

    @Test
    void shouldCreatePipeline() throws Exception {
        login();
        when(pipelineUseCase.create(eq(companyId), any(), any(UUID.class))).thenReturn(pipelineResponse());

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vendas\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vendas"));
    }

    @Test
    void shouldReturn400WhenCreatingPipelineWithoutName() throws Exception {
        login();
        mockMvc.perform(post("/api/v1/companies/" + companyId + "/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetPipelineById() throws Exception {
        login();
        UUID pipelineId = UUID.randomUUID();
        when(pipelineUseCase.getById(companyId, pipelineId)).thenReturn(pipelineResponse());

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/pipelines/" + pipelineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vendas"));
    }

    @Test
    void shouldUpdatePipeline() throws Exception {
        login();
        UUID pipelineId = UUID.randomUUID();
        when(pipelineUseCase.update(eq(companyId), eq(pipelineId), any())).thenReturn(pipelineResponse());

        mockMvc.perform(put("/api/v1/companies/" + companyId + "/pipelines/" + pipelineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vendas 2026\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeletePipeline() throws Exception {
        login();
        UUID pipelineId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/companies/" + companyId + "/pipelines/" + pipelineId))
                .andExpect(status().isNoContent());
        verify(pipelineUseCase).delete(companyId, pipelineId);
    }

    @Test
    void shouldReturnMetrics() throws Exception {
        login();
        UUID pipelineId = UUID.randomUUID();
        when(pipelineUseCase.metrics(companyId, pipelineId)).thenReturn(
                new PipelineMetricsResponse(pipelineId, 1, 1, 0, new java.math.BigDecimal("200"),
                        new java.math.BigDecimal("100"), java.math.BigDecimal.ZERO,
                        new java.math.BigDecimal("1.0"), null, new java.math.BigDecimal("100"), List.of()));

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/pipelines/" + pipelineId + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wonCount").value(1));
    }

    @Test
    void shouldReturn403WhenDeletingPipelineOfOtherCompany() throws Exception {
        login();
        mockMvc.perform(delete("/api/v1/companies/" + UUID.randomUUID() + "/pipelines/" + UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}
