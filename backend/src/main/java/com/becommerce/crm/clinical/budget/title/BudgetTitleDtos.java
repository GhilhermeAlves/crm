package com.becommerce.crm.clinical.budget.title;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** DTOs dos títulos de orçamento salvos. */
public final class BudgetTitleDtos {

    private BudgetTitleDtos() {
    }

    public record BudgetTitleRequest(@NotBlank @Size(max = 255) String title) {
    }

    public record BudgetTitleResponse(UUID id, String title) {
        static BudgetTitleResponse of(BudgetTitleJpaEntity entity) {
            return new BudgetTitleResponse(entity.getId(), entity.getTitle());
        }
    }
}
