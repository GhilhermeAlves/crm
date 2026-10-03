package com.becommerce.crm.analytics.dashboard.application.service;

import com.becommerce.crm.sales.activity.application.port.out.ActivityRepository;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.analytics.dashboard.application.dto.AttentionOpportunity;
import com.becommerce.crm.sales.pipeline.application.port.out.OpportunityRepository;
import com.becommerce.crm.sales.pipeline.application.port.out.PipelineRepository;
import com.becommerce.crm.sales.pipeline.application.port.out.StageRepository;
import com.becommerce.crm.sales.task.application.port.out.TaskRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.sales.pipeline.domain.Opportunity;
import com.becommerce.crm.sales.pipeline.domain.OpportunityStatus;
import com.becommerce.crm.sales.pipeline.domain.Pipeline;
import com.becommerce.crm.sales.pipeline.domain.Stage;
import com.becommerce.crm.sales.task.domain.Task;
import com.becommerce.crm.sales.task.domain.TaskPriority;
import com.becommerce.crm.sales.task.domain.TaskStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock OpportunityRepository opportunityRepository;
    @Mock StageRepository stageRepository;
    @Mock PipelineRepository pipelineRepository;
    @Mock ContactRepository contactRepository;
    @Mock ActivityRepository activityRepository;
    @Mock TaskRepository taskRepository;

    @InjectMocks DashboardService dashboardService;

    private final UUID companyId = UUID.randomUUID();

    private Stage stage(UUID pipelineId, String name, int order, int prob) {
        return Stage.reconstitute(UUID.randomUUID(), pipelineId, companyId, name, null,
                order, prob, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldBuildDashboardHighlightingStaleOpportunity() {
        UUID pipelineId = UUID.randomUUID();
        Stage s1 = stage(pipelineId, "Prospecção", 1, 40);
        Pipeline pipeline = Pipeline.reconstitute(pipelineId, companyId, "Vendas", null, true,
                LocalDateTime.now(), LocalDateTime.now());
        Contact contact = Contact.reconstitute(UUID.randomUUID(), companyId, "Ana", "Souza",
                "ana@e.com", null, null, LocalDateTime.now(), LocalDateTime.now(), null);
        Opportunity opp = Opportunity.reconstitute(UUID.randomUUID(), companyId, "Negócio A",
                new BigDecimal("20000.00"), contact.getId(), pipelineId, s1.getId(), null, null,
                OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(30));

        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of(s1));
        when(pipelineRepository.findByCompanyId(companyId)).thenReturn(List.of(pipeline));
        when(opportunityRepository.findByCompanyId(companyId)).thenReturn(List.of(opp));
        when(activityRepository.findLatestActivityAtByOpportunityId(opp.getId()))
                .thenReturn(Optional.of(LocalDateTime.now().minusDays(10)));
        when(contactRepository.findById(contact.getId())).thenReturn(Optional.of(contact));
        when(activityRepository.findRecentByCompanyId(companyId, DashboardService.RECENT_ACTIVITY_LIMIT))
                .thenReturn(List.of());
        when(taskRepository.findDueToday(eq(companyId), any(), any())).thenReturn(List.of());

        var dashboard = dashboardService.build(companyId);

        assertEquals(1, dashboard.openOpportunities());
        assertEquals(new BigDecimal("20000.00"), dashboard.openValue());
        assertEquals(1, dashboard.attentionOpportunities().size());
        AttentionOpportunity a = dashboard.attentionOpportunities().get(0);
        assertTrue(a.stale());
        assertTrue(a.priorityScore() > 0);
        assertEquals("Ana Souza", a.contactName());
    }

    @Test
    void shouldCountTasksDueTodayExcludingCompleted() {
        Task pending = Task.reconstitute(UUID.randomUUID(), companyId, null, null, "Ligar",
                null, null, LocalDateTime.now(), TaskPriority.HIGH, TaskStatus.PENDING, null,
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
        Task completed = Task.reconstitute(UUID.randomUUID(), companyId, null, null, "Feito",
                null, null, LocalDateTime.now(), TaskPriority.MEDIUM, TaskStatus.COMPLETED,
                LocalDateTime.now(), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());

        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(pipelineRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(opportunityRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(activityRepository.findRecentByCompanyId(companyId, DashboardService.RECENT_ACTIVITY_LIMIT))
                .thenReturn(List.of());
        when(taskRepository.findDueToday(eq(companyId), any(), any())).thenReturn(List.of(pending, completed));

        var dashboard = dashboardService.build(companyId);

        assertEquals(1, dashboard.tasksDueToday());
    }
}