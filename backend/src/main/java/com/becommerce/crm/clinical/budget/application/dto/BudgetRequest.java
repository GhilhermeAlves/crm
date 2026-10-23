package com.becommerce.crm.clinical.budget.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BudgetRequest(@NotBlank @Size(max = 160) String title, @NotEmpty List<UUID> procedureIds,
                            @NotNull @PositiveOrZero Long discountCents, @Size(max = 5000) String notes,
                            Map<UUID, Long> itemPrices) {}
