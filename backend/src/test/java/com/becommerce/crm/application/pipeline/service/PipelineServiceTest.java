package com.becommerce.crm.application.pipeline.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.pipeline.dto.CreatePipelineRequest;
import com.becommerce.crm.application.pipeline.dto.PipelineMetricsResponse;
import com.becommerce.crm.application.pipeline.dto.PipelineResponse;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.PipelineRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Pipeline;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.pipeline.exception.PipelineNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock PipelineRepository pipelineRepository;
    @Mock StageRepository stageRepository;
    @Mock OpportunityRepository opportunityRepository;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks PipelineService pipelineService;

    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(stageRepository.save(any(Stage.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pipeline reqPipeline(String name) {
        return Pipeline.reconstitute(UUID.randomUUID(), companyId, name, null, true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Stage reqStage(UUID pipelineId, String name, int order, int probability) {
        return Stage.reconstitute(UUID.randomUUID(), pipelineId, companyId, name, null,
                order, probability, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldCreatePipelineWithDefaultStages() {
        when(stageRepository.findByPipelineIdOrdered(any(UUID.class))).thenReturn(List.of());

        PipelineResponse response = pipelineService.create(companyId,
                new CreatePipelineRequest("Vendas", null), UUID.randomUUID());

        assertNotNull(response.id());
        assertEquals("Vendas", response.name());
        verify(pipelineRepository).save(any(Pipeline.class));
        verify(stageRepository, times(5)).save(any(Stage.class));
    }

    @Test
    void shouldThrowWhenPipelineNotFoundOnGet() {
        when(pipelineRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThrows(PipelineNotFoundException.class, () -> pipelineService.getById(companyId, UUID.randomUUID()));
    }

    @Test
    void shouldThrowWhenGettingPipelineOfAnotherCompany() {
        Pipeline foreign = Pipeline.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "Outra", null,
                true, LocalDateTime.now(), LocalDateTime.now());
        when(pipelineRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(PipelineNotFoundException.class, () -> pipelineService.getById(companyId, foreign.getId()));
    }

    @Test
    void shouldReturnStagesOnGetById() {
        Pipeline pipeline = reqPipeline("Vendas");
        Stage s1 = reqStage(pipeline.getId(), "Prospecção", 1, 10);
        Stage s2 = reqStage(pipeline.getId(), "Fechamento", 2, 95);
        when(pipelineRepository.findById(pipeline.getId())).thenReturn(Optional.of(pipeline));
        when(stageRepository.findByPipelineIdOrdered(pipeline.getId())).thenReturn(List.of(s1, s2));

        PipelineResponse response = pipelineService.getById(companyId, pipeline.getId());

        assertEquals(2, response.stages().size());
        assertEquals("Prospecção", response.stages().get(0).name());
        assertEquals("Fechamento", response.stages().get(1).name());
    }

    @Test
    void shouldListPipelinesOfCompany() {
        Pipeline p1 = reqPipeline("Vendas");
        Pipeline p2 = reqPipeline("Serviços");
        when(pipelineRepository.findByCompanyId(companyId)).thenReturn(List.of(p1, p2));
        when(stageRepository.findByPipelineIdOrdered(any(UUID.class))).thenReturn(List.of());

        var result = pipelineService.list(companyId);

        assertEquals(2, result.size());
    }

    @Test
    void shouldComputeMetricsWithWinRateAndForecast() {
        Pipeline pipeline = reqPipeline("Vendas");
        Stage s1 = reqStage(pipeline.getId(), "Proposta", 1, 50);
        Stage s2 = reqStage(pipeline.getId(), "Fechamento", 2, 90);

        Opportunity won = Opportunity.reconstitute(UUID.randomUUID(), companyId, "Ganho", new BigDecimal("100.00"),
                UUID.randomUUID(), pipeline.getId(), s2.getId(), null, null, OpportunityStatus.WON,
                LocalDateTime.now(), null, null, null, LocalDateTime.now(), LocalDateTime.now());
        Opportunity open = Opportunity.reconstitute(UUID.randomUUID(), companyId, "Em proposta", new BigDecimal("200.00"),
                UUID.randomUUID(), pipeline.getId(), s1.getId(), null, null, OpportunityStatus.OPEN,
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        Opportunity lost = Opportunity.reconstitute(UUID.randomUUID(), companyId, "Perdida", new BigDecimal("50.00"),
                UUID.randomUUID(), pipeline.getId(), s2.getId(), null, null, OpportunityStatus.LOST,
                null, LocalDateTime.now(), "Preço", null, LocalDateTime.now(), LocalDateTime.now());

        when(pipelineRepository.findById(pipeline.getId())).thenReturn(Optional.of(pipeline));
        when(stageRepository.findByPipelineIdOrdered(pipeline.getId())).thenReturn(List.of(s1, s2));
        when(opportunityRepository.findByPipelineId(pipeline.getId())).thenReturn(List.of(won, open, lost));

        PipelineMetricsResponse metrics = pipelineService.metrics(companyId, pipeline.getId());

        assertEquals(1, metrics.wonCount());
        assertEquals(1, metrics.lostCount());
        assertEquals(1, metrics.openCount());
        assertEquals(0.5, metrics.winRate().doubleValue());
        assertEquals(new BigDecimal("200.00"), metrics.totalValue());
        // forecast = 200 * 0.5 = 100
        assertEquals(0, new BigDecimal("100.00").compareTo(metrics.forecast()));
        assertEquals(2, metrics.byStage().size());
    }
}
