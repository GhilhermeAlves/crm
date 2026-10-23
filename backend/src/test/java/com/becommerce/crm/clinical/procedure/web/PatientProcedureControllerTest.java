package com.becommerce.crm.clinical.procedure.web;

import com.becommerce.crm.clinical.procedure.application.dto.CompleteProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.port.in.PatientProcedureUseCase;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PatientProcedureControllerTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID PROC_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private PatientProcedureUseCase useCase;
    private PatientProcedureController controller;
    private PatientProcedureRequest request;

    @BeforeEach
    void setUp() {
        useCase = mock(PatientProcedureUseCase.class);
        controller = new PatientProcedureController(useCase);
        request = mock(PatientProcedureRequest.class);
    }

    private static CurrentUser principalWith(String... perms) {
        return new CurrentUser(USER_ID, "a@b.c", COMPANY_ID, COMPANY_ID, List.of(), List.of(perms),
                "sub-1", null, "keycloak", "Ana", "AGENT");
    }

    @Test
    void createPassesFinancePermission() {
        controller.create(COMPANY_ID, CONTACT_ID, request, principalWith("contact:update", "finance:create"));
        verify(useCase).create(COMPANY_ID, CONTACT_ID, request, USER_ID, true);
    }

    @Test
    void createWithoutFinancePermissionPassesFalse() {
        controller.create(COMPANY_ID, CONTACT_ID, request, principalWith("contact:update"));
        verify(useCase).create(COMPANY_ID, CONTACT_ID, request, USER_ID, false);
    }

    @Test
    void createReturns201() {
        assertThat(controller.create(COMPANY_ID, CONTACT_ID, request, principalWith("contact:update"))
                .getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void completePassesFinancePermission() {
        CompleteProcedureRequest req = mock(CompleteProcedureRequest.class);
        controller.complete(COMPANY_ID, CONTACT_ID, PROC_ID, req, principalWith("finance:create"));
        verify(useCase).complete(COMPANY_ID, CONTACT_ID, PROC_ID, req, USER_ID, true);
    }

    @Test
    void listDelegatesFilters() {
        controller.list(COMPANY_ID, CONTACT_ID, ProcedureState.PLANNED, "cat", 11);
        verify(useCase).list(COMPANY_ID, CONTACT_ID, ProcedureState.PLANNED, "cat", 11);
    }

    @Test
    void deleteReturnsNoContent() {
        assertThat(controller.delete(COMPANY_ID, CONTACT_ID, PROC_ID, principalWith("contact:update"))
                .getStatusCode().value()).isEqualTo(204);
        verify(useCase).delete(COMPANY_ID, CONTACT_ID, PROC_ID, USER_ID);
    }
}
