package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.activity.Activity;
import com.becommerce.crm.domain.activity.ActivityType;
import com.becommerce.crm.domain.ai.AiRecordType;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityHistory;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpportunityContextResolverTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();

    private final OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
    private final StageRepository stageRepository = mock(StageRepository.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final OpportunityContextResolver resolver = new OpportunityContextResolver(
            opportunityRepository, stageRepository, contactRepository,
            activityRepository, taskRepository, userRepository);

    private OpportunityContextResolverTestFixture fixture() {
        UUID stageId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        Stage stage = Stage.reconstitute(stageId, UUID.randomUUID(), companyId, "Proposta",
                null, 3, 60, LocalDateTime.now(), LocalDateTime.now());
        Contact contact = Contact.reconstitute(contactId, companyId, "Ana", "Souza",
                "ana@e.com", null, null, LocalDateTime.now(), LocalDateTime.now(), null);
        User assignee = new User();
        assignee.setId(assigneeId);
        assignee.setName("Maria Lima");
        Opportunity opp = Opportunity.reconstitute(recordId, companyId, "Negócio ABC",
                new BigDecimal("50000.00"), contactId, UUID.randomUUID(), stageId, assigneeId,
                LocalDateTime.now().plusDays(15), OpportunityStatus.OPEN, null, null, null,
                "cliente pediu proposta", LocalDateTime.now().minusDays(30), LocalDateTime.now());
        Activity activity = Activity.reconstitute(UUID.randomUUID(), companyId, contactId, recordId,
                ActivityType.CALL, "Chamada de follow-up", "conversamos sobre prazos",
                LocalDateTime.now().minusDays(1), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
        Task task = Task.reconstitute(UUID.randomUUID(), companyId, contactId, recordId,
                "Enviar proposta", null, assigneeId, LocalDateTime.now(), TaskPriority.HIGH,
                TaskStatus.PENDING, null, assigneeId, LocalDateTime.now(), LocalDateTime.now());
        OpportunityHistory moved = OpportunityHistory.reconstitute(UUID.randomUUID(), recordId,
                UUID.randomUUID(), stageId, assigneeId, LocalDateTime.now().minusDays(18), null);

        when(opportunityRepository.findById(recordId)).thenReturn(Optional.of(opp));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(activityRepository.findByOpportunityId(recordId)).thenReturn(List.of(activity));
        when(taskRepository.findByCompanyIdAndOpportunityId(companyId, recordId)).thenReturn(List.of(task));
        when(opportunityRepository.findHistoryByOpportunityId(recordId)).thenReturn(List.of(moved));

        return new OpportunityContextResolverTestFixture(opp, stage, contact, assignee, activity, task, moved);
    }

    @Test
    void shouldSupportOpportunityTypeAndPermission() {
        assertEquals(AiRecordType.OPPORTUNITY, resolver.type());
        assertEquals("opportunity:read", resolver.requiredPermission());
    }

    @Test
    void shouldBuildStructuredFactsFromRealCrmData() {
        fixture();
        List<com.becommerce.crm.application.ai.dto.AiFact> facts = resolver.facts(companyId, recordId);

        assertFalse(facts.isEmpty());
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.title") && f.value().equals("Negócio ABC")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.value") && f.value().contains("50000")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.stage") && f.value().equals("Proposta")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.probability") && f.value().equals("60%")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.status") && f.value().equals("ABERTA")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.contact") && f.value().contains("Ana")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.assignee") && f.value().contains("Maria")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.notes") && f.value().contains("proposta")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.activities")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.tasks")));
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.history")));

        // Tempo no estágio derivado do movimento mais recente para o estágio atual.
        assertTrue(facts.stream().anyMatch(f -> f.key().equals("opportunity.time_in_stage")
                && f.value().contains("18")));

        // Todos os fatos apontam a mesma fonte.
        assertTrue(facts.stream().allMatch(f -> f.source().equals("opportunity_context")));
    }

    @Test
    void shouldNotInventTimeInStageWhenHistoryCannotDetermineIt() {
        Opportunity opp = Opportunity.reconstitute(recordId, companyId, "Sem histórico",
                new BigDecimal("1000.00"), null, UUID.randomUUID(), UUID.randomUUID(), null,
                null, OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(opportunityRepository.findById(recordId)).thenReturn(Optional.of(opp));
        when(opportunityRepository.findHistoryByOpportunityId(recordId)).thenReturn(List.of());
        when(activityRepository.findByOpportunityId(recordId)).thenReturn(List.of());
        when(taskRepository.findByCompanyIdAndOpportunityId(companyId, recordId)).thenReturn(List.of());

        List<com.becommerce.crm.application.ai.dto.AiFact> facts = resolver.facts(companyId, recordId);

        assertTrue(facts.stream().noneMatch(f -> f.key().equals("opportunity.time_in_stage")),
                "sem histórico determinável, o tempo no estágio não deve ser inventado");
    }

    @Test
    void shouldReturnEmptyFactsWhenOpportunityNotFound() {
        when(opportunityRepository.findById(recordId)).thenReturn(Optional.empty());
        assertTrue(resolver.facts(companyId, recordId).isEmpty());
        verify(activityRepository, never()).findByOpportunityId(any());
    }

    @Test
    void shouldEnrichResolveWithResponsibleActivitiesTasksHistory() {
        fixture();
        String ctx = resolver.resolve(companyId, recordId);
        assertNotNull(ctx);
        assertTrue(ctx.contains("Maria Lima"));
        assertTrue(ctx.contains("Chamada de follow-up"));
        assertTrue(ctx.contains("Enviar proposta"));
        assertTrue(ctx.contains("Histórico"));
        assertTrue(ctx.contains("18"));
    }

    @Test
    void shouldFetchEachResourceWithASingleQueryNoPerItemLoop() {
        fixture();
        resolver.facts(companyId, recordId);

        // N+1 guard: uma query por recurso, não por item (nenhum loop por atividade/tarefa/histórico).
        verify(activityRepository).findByOpportunityId(recordId);
        verify(taskRepository).findByCompanyIdAndOpportunityId(companyId, recordId);
        verify(opportunityRepository).findHistoryByOpportunityId(recordId);
        verifyNoMoreInteractions(activityRepository);
    }

    private record OpportunityContextResolverTestFixture(Opportunity opp, Stage stage, Contact contact,
                                                         User assignee, Activity activity, Task task,
                                                         OpportunityHistory moved) {
    }
}