package com.becommerce.crm.clinical.budget.web;

import com.becommerce.crm.clinical.budget.application.dto.ApproveBudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetRequest;
import com.becommerce.crm.clinical.budget.application.port.in.BudgetUseCase;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BudgetControllerTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID BUDGET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private BudgetUseCase useCase;
    private BudgetController controller;

    @BeforeEach
    void setUp() {
        useCase = mock(BudgetUseCase.class);
        controller = new BudgetController(useCase);
    }

    private static CurrentUser principalWith(String... perms) {
        return new CurrentUser(USER_ID, "a@b.c", COMPANY_ID, COMPANY_ID, List.of(), List.of(perms),
                "sub-1", null, "keycloak", "Ana", "AGENT");
    }

    @Test
    void approvePassesFinancePermission() {
        ApproveBudgetRequest req = mock(ApproveBudgetRequest.class);
        controller.approve(COMPANY_ID, CONTACT_ID, BUDGET_ID, req, principalWith("contact:update", "finance:create"));
        verify(useCase).approve(COMPANY_ID, CONTACT_ID, BUDGET_ID, req, USER_ID, true);
    }

    @Test
    void approveWithoutFinancePermissionPassesFalse() {
        ApproveBudgetRequest req = mock(ApproveBudgetRequest.class);
        controller.approve(COMPANY_ID, CONTACT_ID, BUDGET_ID, req, principalWith("contact:update"));
        verify(useCase).approve(COMPANY_ID, CONTACT_ID, BUDGET_ID, req, USER_ID, false);
    }

    @Test
    void createReturns201() {
        BudgetRequest req = mock(BudgetRequest.class);
        assertThat(controller.create(COMPANY_ID, CONTACT_ID, req, principalWith("contact:update"))
                .getStatusCode().value()).isEqualTo(201);
        verify(useCase).create(COMPANY_ID, CONTACT_ID, req, USER_ID);
    }

    @Test
    void loseDelegates() {
        controller.lose(COMPANY_ID, CONTACT_ID, BUDGET_ID, principalWith("contact:update"));
        verify(useCase).lose(COMPANY_ID, CONTACT_ID, BUDGET_ID, USER_ID);
    }

    @Test
    void deleteReturnsNoContent() {
        assertThat(controller.delete(COMPANY_ID, CONTACT_ID, BUDGET_ID, principalWith("contact:update"))
                .getStatusCode().value()).isEqualTo(204);
        verify(useCase).delete(COMPANY_ID, CONTACT_ID, BUDGET_ID, USER_ID);
    }
}
