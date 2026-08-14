package com.becommerce.crm.application.pipeline.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.dto.CreateOpportunityRequest;
import com.becommerce.crm.application.pipeline.dto.MarkLostRequest;
import com.becommerce.crm.application.pipeline.dto.MoveDirection;
import com.becommerce.crm.application.pipeline.dto.MoveOpportunityRequest;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.PipelineRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityHistory;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Pipeline;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.pipeline.exception.PipelineValidationException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
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
class OpportunityServiceTest {

    @Mock OpportunityRepository opportunityRepository;
    @Mock PipelineRepository pipelineRepository;
    @Mock StageRepository stageRepository;
    @Mock ContactRepository contactRepository;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks OpportunityService opportunityService;

    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(stageRepository.save(any(Stage.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private Contact ownedContact() {
        return Contact.reconstitute(UUID.randomUUID(), companyId, "Ana", "Souza", "ana@e.com",
                null, null, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private Stage stage(UUID pipelineId, String name, int order) {
        return Stage.reconstitute(UUID.randomUUID(), pipelineId, companyId, name, null,
                order, 50, LocalDateTime.now(), LocalDateTime.now());
    }

    private Opportunity opportunity(UUID pipelineId, UUID stageId) {
        return Opportunity.reconstitute(UUID.randomUUID(), companyId, "Negócio", new BigDecimal("150.00"),
                UUID.randomUUID(), pipelineId, stageId, null, null, OpportunityStatus.OPEN,
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    private void stubPipelineOwned(UUID pipelineId) {
        Pipeline pipeline = Pipeline.reconstitute(pipelineId, companyId, "Vendas", null, true,
                LocalDateTime.now(), LocalDateTime.now());
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));
    }

    @Test
    void shouldCreateOpportunityAtFirstStage() {
        UUID pipelineId = UUID.randomUUID();
        Contact contact = ownedContact();
        Stage s1 = stage(pipelineId, "Prospecção", 1);
        Stage s2 = stage(pipelineId, "Qualificação", 2);

        stubPipelineOwned(pipelineId);
        when(contactRepository.findById(contact.getId())).thenReturn(Optional.of(contact));
        when(stageRepository.findByPipelineIdOrdered(pipelineId)).thenReturn(List.of(s1, s2));

        var response = opportunityService.create(companyId, pipelineId,
                new CreateOpportunityRequest("Oportunidade A", new BigDecimal("150.00"), contact.getId(),
                        null, null, null), UUID.randomUUID());

        assertEquals(OpportunityStatus.OPEN, response.status());
        assertEquals(s1.getId(), response.stageId());
        assertEquals("Prospecção", response.stageName());
        verify(opportunityRepository).saveHistory(any(OpportunityHistory.class));
    }

    @Test
    void shouldRejectCreateWhenContactBelongsToAnotherCompany() {
        UUID pipelineId = UUID.randomUUID();
        Contact foreign = Contact.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "Ana", "Souza",
                "ana@e.com", null, null, LocalDateTime.now(), LocalDateTime.now(), null);

        stubPipelineOwned(pipelineId);
        when(contactRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(ContactNotFoundException.class, () -> opportunityService.create(companyId, pipelineId,
                new CreateOpportunityRequest("X", new BigDecimal("10.00"), foreign.getId(), null, null, null),
                UUID.randomUUID()));
        verify(opportunityRepository, never()).save(any(Opportunity.class));
    }

    @Test
    void shouldAdvanceOpportunityByOneStage() {
        UUID pipelineId = UUID.randomUUID();
        Stage s1 = stage(pipelineId, "Prospecção", 1);
        Stage s2 = stage(pipelineId, "Qualificação", 2);
        Opportunity opp = opportunity(pipelineId, s1.getId());

        when(opportunityRepository.findById(opp.getId())).thenReturn(Optional.of(opp));
        when(stageRepository.findByPipelineIdOrdered(pipelineId)).thenReturn(List.of(s1, s2));

        var response = opportunityService.move(companyId, opp.getId(),
                new MoveOpportunityRequest(MoveDirection.ADVANCE, null), UUID.randomUUID());

        assertEquals(s2.getId(), response.stageId());
        assertEquals("Qualificação", response.stageName());
        verify(opportunityRepository).saveHistory(any(OpportunityHistory.class));
    }

    @Test
    void shouldRejectRegressOnFirstStage() {
        UUID pipelineId = UUID.randomUUID();
        Stage s1 = stage(pipelineId, "Prospecção", 1);
        Stage s2 = stage(pipelineId, "Qualificação", 2);
        Opportunity opp = opportunity(pipelineId, s1.getId());

        when(opportunityRepository.findById(opp.getId())).thenReturn(Optional.of(opp));
        when(stageRepository.findByPipelineIdOrdered(pipelineId)).thenReturn(List.of(s1, s2));

        assertThrows(PipelineValidationException.class, () -> opportunityService.move(companyId, opp.getId(),
                new MoveOpportunityRequest(MoveDirection.REGRESS, null), UUID.randomUUID()));
        verify(opportunityRepository, never()).save(any(Opportunity.class));
    }

    @Test
    void shouldRejectAdvanceWhenAtLastStage() {
        UUID pipelineId = UUID.randomUUID();
        Stage s1 = stage(pipelineId, "Prospecção", 1);
        Stage s2 = stage(pipelineId, "Qualificação", 2);
        Opportunity opp = opportunity(pipelineId, s2.getId());

        when(opportunityRepository.findById(opp.getId())).thenReturn(Optional.of(opp));
        when(stageRepository.findByPipelineIdOrdered(pipelineId)).thenReturn(List.of(s1, s2));

        assertThrows(PipelineValidationException.class, () -> opportunityService.move(companyId, opp.getId(),
                new MoveOpportunityRequest(MoveDirection.ADVANCE, null), UUID.randomUUID()));
    }

    @Test
    void shouldRejectWinWhenNotAtLastStage() {
        UUID pipelineId = UUID.randomUUID();
        Stage s1 = stage(pipelineId, "Prospecção", 1);
        Stage s2 = stage(pipelineId, "Qualificação", 2);
        Opportunity opp = opportunity(pipelineId, s1.getId());

        when(opportunityRepository.findById(opp.getId())).thenReturn(Optional.of(opp));
        when(stageRepository.findByPipelineIdOrdered(pipelineId)).thenReturn(List.of(s1, s2));

        assertThrows(PipelineValidationException.class,
                () -> opportunityService.markWon(companyId, opp.getId(), UUID.randomUUID()));
    }

    @Test
    void shouldRejectLostWithoutReason() {
        UUID pipelineId = UUID.randomUUID();
        Opportunity opp = opportunity(pipelineId, UUID.randomUUID());

        assertThrows(PipelineValidationException.class, () -> opportunityService.markLost(companyId, opp.getId(),
                new MarkLostRequest(" "), UUID.randomUUID()));
        verify(opportunityRepository, never()).save(any(Opportunity.class));
    }

    @Test
    void shouldMarkLostAtLastStage() {
        UUID pipelineId = UUID.randomUUID();
        Stage s1 = stage(pipelineId, "Prospecção", 1);
        Stage s2 = stage(pipelineId, "Qualificação", 2);
        Opportunity opp = opportunity(pipelineId, s2.getId());

        when(opportunityRepository.findById(opp.getId())).thenReturn(Optional.of(opp));
        when(stageRepository.findByPipelineIdOrdered(pipelineId)).thenReturn(List.of(s1, s2));
        when(stageRepository.findById(s2.getId())).thenReturn(Optional.of(s2));

        var response = opportunityService.markLost(companyId, opp.getId(),
                new MarkLostRequest("Preço alto"), UUID.randomUUID());

        assertEquals(OpportunityStatus.LOST, response.status());
        assertEquals("Preço alto", response.lossReason());
        verify(opportunityRepository).save(any(Opportunity.class));
    }
}
