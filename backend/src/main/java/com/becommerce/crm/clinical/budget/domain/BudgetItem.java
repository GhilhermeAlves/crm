package com.becommerce.crm.clinical.budget.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.util.UUID;

/** Procedimento incluído no orçamento, com o valor congelado no momento da inclusão. */
public record BudgetItem(UUID id, UUID procedureId, long priceCents, int position) {

    public BudgetItem {
        if (priceCents < 0) {
            throw new ContactValidationException("O valor do item não pode ser negativo.");
        }
    }
}
