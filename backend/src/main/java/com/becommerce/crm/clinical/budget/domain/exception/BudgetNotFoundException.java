package com.becommerce.crm.clinical.budget.domain.exception;

import java.util.UUID;

public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(UUID id) {
        super("Orçamento não encontrado: " + id);
    }
}
