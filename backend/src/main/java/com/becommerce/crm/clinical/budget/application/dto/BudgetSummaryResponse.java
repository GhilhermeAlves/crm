package com.becommerce.crm.clinical.budget.application.dto;

import com.becommerce.crm.clinical.budget.domain.BudgetStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record BudgetSummaryResponse(UUID id, String title, BudgetStatus status, long subtotalCents,
                                    long discountCents, long totalCents, LocalDateTime updatedAt,
                                    String updatedByName) {}
