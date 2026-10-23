package com.becommerce.crm.clinical.procedure.application.dto;

import com.becommerce.crm.clinical.procedure.domain.Dentition;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.clinical.budget.domain.BudgetStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PatientProcedureResponse(UUID id, UUID contactId, UUID catalogItemId, String name, String category,
        ProcedureState state, Dentition dentition, long priceCents, UUID professionalId, String professionalName,
        String notes, LocalDate performedAt, List<ProcedureToothDto> teeth, TransactionRef transaction,
        LocalDateTime createdAt, LocalDateTime updatedAt, BudgetRef budget) {

    public record TransactionRef(UUID id, TransactionStatus status) {}

    public record BudgetRef(UUID id, String title, BudgetStatus status) {}
}
