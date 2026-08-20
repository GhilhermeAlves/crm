package com.becommerce.crm.application.customer360.service;

import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.PipelineRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.activity.Activity;
import com.becommerce.crm.domain.activity.ActivityType;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Pipeline;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Customer360ServiceTest {

    @Mock OpportunityRepository opportunityRepository;
    @Mock StageRepository stageRepository;
    @Mock PipelineRepository pipelineRepository;
    @Mock ContactRepository contactRepository;
    @Mock ActivityRepository activityRepository;
    @Mock TaskRepository taskRepository;

    @InjectMocks Customer360Service customer360Service;

    private final UUID companyId = UUID.randomUUID();

    private Contact contact(LocalDateTime createdAt) {
        return Contact.reconstitute(UUID.randomUUID(), companyId, "Ana", "Souza",
                "ana@e.com", "11-99999", "leads quentes", createdAt, createdAt, null);
    }

    private Stage stage(UUID pipelineId, String name, int order, int prob) {
        return Stage.reconstitute(UUID.randomUUID(), pipelineId, companyId, name, null,
                order, prob, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldReturnComplete360ForContactWithData() {
        UUID pipelineId = UUID.randomUUID();
        Stage stage = stage(pipelineId, "Proposta", 3, 60);
        Pipeline pipeline = Pipeline.reconstitute(pipelineId, companyId, "Vendas", null, true,
                LocalDateTime.now(), LocalDateTime.now());
        Contact c = contact(LocalDateTime.now());
        Opportunity open = Opportunity.reconstitute(UUID.randomUUID(), companyId, "Negócio A",
                new BigDecimal("5000.00"), c.getId(), pipelineId, stage.getId(), null,
                LocalDateTime.now().plusDays(10), OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        Task task = Task.reconstitute(UUID.randomUUID(), companyId, c.getId(), null, "Ligar para Ana",
                null, null, LocalDateTime.now(), TaskPriority.HIGH, TaskStatus.PENDING, null,
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
        Activity activity = Activity.reconstitute(UUID.randomUUID(), companyId, c.getId(), open.getId(),
                ActivityType.CALL, "Chamada", "falei sobre proposta", LocalDateTime.now().minusDays(1),
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());

        when(contactRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of(stage));
        when(pipelineRepository.findByCompanyId(companyId)).thenReturn(List.of(pipeline));
        when(opportunityRepository.findByContactId(c.getId())).thenReturn(List.of(open));
        when(taskRepository.findByContactId(c.getId())).thenReturn(List.of(task));
        when(opportunityRepository.findHistoryByOpportunityIds(java.util.List.of(open.getId())))
                .thenReturn(java.util.Map.of());
        when(activityRepository.findByContactId(c.getId())).thenReturn(List.of(activity));
        when(activityRepository.findLatestActivityAtByContactId(c.getId()))
                .thenReturn(Optional.of(LocalDateTime.now().minusDays(1)));
        when(activityRepository.findLatestActivityAtByOpportunityIds(anyCollection()))
                .thenReturn(java.util.Map.of(open.getId(), LocalDateTime.now().minusDays(1)));

        var result = customer360Service.build(companyId, c.getId());

        assertEquals(c.getId(), result.contact().id());
        assertEquals("Ana Souza", result.contact().fullName());
        assertEquals(1, result.openOpportunities());
        assertEquals(new BigDecimal("5000.00"), result.openValue());
        assertEquals(1, result.opportunities().size());
        assertEquals("Negócio A", result.opportunities().get(0).title());
        assertEquals("Proposta", result.opportunities().get(0).stageName());
        assertEquals(1, result.tasks().size());
        assertTrue(result.tasks().get(0).title().contains("Ligar"));
        assertFalse(result.contact().atRisk());
        // Linha do tempo: atividade + tarefa criada + oportunidade criada.
        assertTrue(result.timeline().size() >= 3);
        assertTrue(result.timeline().stream().anyMatch(t -> t.type().equals("ACTIVITY")));
        assertTrue(result.timeline().stream().anyMatch(t -> t.type().equals("OPPORTUNITY_CREATED")));
    }

    @Test
    void shouldFlagStaleOpportunityAsAtRiskAndRecommendFollowUp() {
        UUID pipelineId = UUID.randomUUID();
        Stage stage = stage(pipelineId, "Prospecção", 1, 40);
        Pipeline pipeline = Pipeline.reconstitute(pipelineId, companyId, "Vendas", null, true,
                LocalDateTime.now(), LocalDateTime.now());
        Contact c = contact(LocalDateTime.now().minusDays(30));
        Opportunity stale = Opportunity.reconstitute(UUID.randomUUID(), companyId, "Negócio parado",
                new BigDecimal("8000.00"), c.getId(), pipelineId, stage.getId(), null, null,
                OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(20));

        when(contactRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of(stage));
        when(pipelineRepository.findByCompanyId(companyId)).thenReturn(List.of(pipeline));
        when(opportunityRepository.findByContactId(c.getId())).thenReturn(List.of(stale));
        when(taskRepository.findByContactId(c.getId())).thenReturn(List.of());
        when(opportunityRepository.findHistoryByOpportunityIds(java.util.List.of(stale.getId())))
                .thenReturn(java.util.Map.of());
        when(activityRepository.findByContactId(c.getId())).thenReturn(List.of());
        when(activityRepository.findLatestActivityAtByContactId(c.getId()))
                .thenReturn(Optional.of(LocalDateTime.now().minusDays(10)));
        when(activityRepository.findLatestActivityAtByOpportunityIds(anyCollection()))
                .thenReturn(java.util.Map.of(stale.getId(), LocalDateTime.now().minusDays(10)));

        var result = customer360Service.build(companyId, c.getId());

        assertTrue(result.contact().atRisk());
        assertTrue(result.contact().riskMessage().contains("10"));
        assertEquals("FOLLOW_UP", result.nextAction().type());
        assertEquals(90, result.nextAction().priority());
    }

    @Test
    void shouldRecommendCompletingUrgentOverdueTask() {
        Stage stage = stage(UUID.randomUUID(), "Prospecção", 1, 40);
        Pipeline pipeline = Pipeline.reconstitute(UUID.randomUUID(), companyId, "Vendas", null, true,
                LocalDateTime.now(), LocalDateTime.now());
        Contact c = contact(LocalDateTime.now());
        Opportunity open = Opportunity.reconstitute(UUID.randomUUID(), companyId, "Negócio A",
                new BigDecimal("2000.00"), c.getId(), stage.getPipelineId(), stage.getId(),
                null, null, OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        Task overdue = Task.reconstitute(UUID.randomUUID(), companyId, c.getId(), null, "Enviar proposta",
                null, null, LocalDateTime.now().minusDays(2), TaskPriority.HIGH, TaskStatus.PENDING,
                null, UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());

        when(contactRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of(stage));
        when(pipelineRepository.findByCompanyId(companyId)).thenReturn(List.of(pipeline));
        when(opportunityRepository.findByContactId(c.getId())).thenReturn(List.of(open));
        when(taskRepository.findByContactId(c.getId())).thenReturn(List.of(overdue));
        when(opportunityRepository.findHistoryByOpportunityIds(java.util.List.of(open.getId())))
                .thenReturn(java.util.Map.of());
        when(activityRepository.findByContactId(c.getId())).thenReturn(List.of());
        when(activityRepository.findLatestActivityAtByContactId(c.getId()))
                .thenReturn(Optional.of(LocalDateTime.now().minusDays(1)));
        when(activityRepository.findLatestActivityAtByOpportunityIds(anyCollection()))
                .thenReturn(java.util.Map.of(open.getId(), LocalDateTime.now().minusDays(1)));

        var result = customer360Service.build(companyId, c.getId());

        assertEquals("COMPLETE_TASK", result.nextAction().type());
        assertTrue(result.tasks().get(0).overdue());
    }

    @Test
    void shouldReturnEmpty360WhenContactHasNoData() {
        Contact c = contact(LocalDateTime.now());

        when(contactRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(stageRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(pipelineRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(opportunityRepository.findByContactId(c.getId())).thenReturn(List.of());
        when(taskRepository.findByContactId(c.getId())).thenReturn(List.of());
        when(activityRepository.findByContactId(any())).thenReturn(List.of());
        when(activityRepository.findLatestActivityAtByContactId(c.getId()))
                .thenReturn(Optional.of(LocalDateTime.now()));

        var result = customer360Service.build(companyId, c.getId());

        assertEquals(0, result.openOpportunities());
        assertEquals(BigDecimal.ZERO, result.openValue());
        assertTrue(result.opportunities().isEmpty());
        assertTrue(result.tasks().isEmpty());
        assertFalse(result.contact().atRisk());
        assertEquals("NONE", result.nextAction().type());
    }

    @Test
    void shouldThrowWhenContactBelongsToAnotherCompany() {
        UUID otherCompany = UUID.randomUUID();
        Contact c = Contact.reconstitute(UUID.randomUUID(), otherCompany, "Ana", "Souza",
                "ana@e.com", null, null, LocalDateTime.now(), LocalDateTime.now(), null);

        when(contactRepository.findById(c.getId())).thenReturn(Optional.of(c));

        assertThrows(ContactNotFoundException.class,
                () -> customer360Service.build(companyId, c.getId()));
    }

    @Test
    void shouldThrowWhenContactNotFound() {
        UUID missing = UUID.randomUUID();
        when(contactRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(ContactNotFoundException.class,
                () -> customer360Service.build(companyId, missing));
    }
}