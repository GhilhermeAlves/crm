package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionExecutor;
import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.domain.AiAction;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.sales.activity.application.port.in.ActivityUseCase;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.task.application.port.in.TaskUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreatePatientToolTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, userId,
            new AiPermissionContext(List.of("contact:create")), conversationId);

    private final AiActionService actionService = mock(AiActionService.class);
    private final ContactUseCase contacts = mock(ContactUseCase.class);
    private final CreatePatientTool tool = new CreatePatientTool(actionService, contacts);

    @BeforeEach
    void setUp() {
        when(contacts.search(eq(companyId), anyString(), anyInt())).thenReturn(List.of());
    }

    @Test
    void permissionIsContactCreate() {
        assertEquals("contact:create", tool.requiredPermission());
        assertEquals("WRITE", tool.risk());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validProposalSplitsFullName() {
        tool.execute(ctx, Map.of("firstName", "Lúcia de Aguiar da Silva", "mobile", "(11) 98888-7777",
                "email", "lucia@exemplo.com", "birthDate", "1985-03-12", "cpf", "123.456.789-09"));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId), eq("create_patient"),
                eq("CONTACT"), eq(null), params.capture(), description.capture());
        assertEquals("Lúcia", params.getValue().get("firstName"));
        assertEquals("de Aguiar da Silva", params.getValue().get("lastName"));
        assertEquals("1985-03-12", params.getValue().get("birthDate"));
        assertFalse(params.getValue().containsKey("duplicateOf"));
        assertTrue(description.getValue().startsWith("Cadastrar paciente: Lúcia de Aguiar da Silva"));
        assertTrue(description.getValue().contains("nascimento 12/03/1985"));
        assertFalse(description.getValue().contains("Já existe"));
    }

    @Test
    void explicitLastNameIsKept() {
        String[] name = CreatePatientTool.splitName("Lúcia", "de Aguiar da Silva");
        assertEquals("Lúcia", name[0]);
        assertEquals("de Aguiar da Silva", name[1]);
        String[] single = CreatePatientTool.splitName("Lúcia", null);
        assertEquals(null, single[1]);
    }

    @Test
    @SuppressWarnings("unchecked")
    void warnsAboutDuplicateByFullName() {
        UUID existingId = UUID.randomUUID();
        ContactResponse existing = mock(ContactResponse.class);
        when(existing.id()).thenReturn(existingId);
        when(existing.firstName()).thenReturn("Lucia");
        when(existing.lastName()).thenReturn("de Aguiar da Silva");
        when(contacts.search(companyId, "Lúcia", 50)).thenReturn(List.of(existing));

        tool.execute(ctx, Map.of("firstName", "Lúcia", "lastName", "de Aguiar da Silva"));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(actionService).propose(any(), any(), any(), anyString(), anyString(), any(), params.capture(),
                description.capture());
        assertEquals(List.of(existingId.toString()), params.getValue().get("duplicateOf"));
        assertTrue(description.getValue().contains("Já existe um paciente chamado Lucia de Aguiar da Silva"));
    }

    @Test
    void rejectsInvalidBirthDate() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.execute(ctx, Map.of("firstName", "Lúcia", "birthDate", "12/03/1985")));
        verifyNoProposal();
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.execute(ctx, Map.of("firstName", "Lúcia", "email", "lucia-sem-arroba")));
        verifyNoProposal();
    }

    @Test
    void rejectsMissingName() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx, Map.of("firstName", "  ")));
        verifyNoProposal();
    }

    @Test
    void executorCreatesPatient() {
        AiActionExecutor executor = new AiActionExecutor(mock(TaskUseCase.class), mock(ActivityUseCase.class),
                mock(OpportunityUseCase.class), mock(AppointmentUseCase.class),
                mock(FinancialTransactionService.class), mock(InventoryService.class), contacts);
        AiAction action = mock(AiAction.class);
        when(action.getTool()).thenReturn("create_patient");
        when(action.getCompanyId()).thenReturn(companyId);
        when(action.getUserId()).thenReturn(userId);
        when(action.getParameters()).thenReturn(Map.of("firstName", "Lúcia", "lastName", "de Aguiar da Silva",
                "mobile", "11988887777", "email", "lucia@exemplo.com", "birthDate", "1985-03-12",
                "cpf", "12345678909", "notes", "Indicação", "duplicateOf", List.of()));

        executor.execute(action);

        ArgumentCaptor<CreateContactRequest> request = ArgumentCaptor.forClass(CreateContactRequest.class);
        verify(contacts).createPatient(eq(companyId), request.capture(), eq(userId));
        CreateContactRequest r = request.getValue();
        assertEquals("Lúcia", r.firstName());
        assertEquals("de Aguiar da Silva", r.lastName());
        assertEquals("lucia@exemplo.com", r.email());
        assertEquals(null, r.phone());
        assertEquals("11988887777", r.mobile());
        assertEquals("Indicação", r.notes());
        assertEquals(LocalDate.of(1985, 3, 12), r.birthDate());
        assertEquals("12345678909", r.cpf());
        assertEquals(null, r.guardianPhone());
        verify(contacts, never()).create(any(), any(), any());
    }

    private void verifyNoProposal() {
        verify(actionService, never()).propose(any(), any(), any(), anyString(), anyString(), any(), any(),
                anyString());
    }
}
