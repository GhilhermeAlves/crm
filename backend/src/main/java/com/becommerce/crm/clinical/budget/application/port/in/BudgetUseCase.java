package com.becommerce.crm.clinical.budget.application.port.in;

import com.becommerce.crm.clinical.budget.application.dto.ApproveBudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetResponse;
import com.becommerce.crm.clinical.budget.application.dto.BudgetSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface BudgetUseCase {

    List<BudgetSummaryResponse> list(UUID companyId, UUID contactId);

    BudgetResponse get(UUID companyId, UUID contactId, UUID budgetId);

    BudgetResponse create(UUID companyId, UUID contactId, BudgetRequest request, UUID actorId);

    BudgetResponse update(UUID companyId, UUID contactId, UUID budgetId, BudgetRequest request, UUID actorId);

    BudgetResponse approve(UUID companyId, UUID contactId, UUID budgetId, ApproveBudgetRequest request,
                           UUID actorId, boolean canCreateFinance);

    BudgetResponse lose(UUID companyId, UUID contactId, UUID budgetId, UUID actorId);

    void delete(UUID companyId, UUID contactId, UUID budgetId, UUID actorId);
}
