package com.becommerce.crm.clinical.budget.application.dto;

import com.becommerce.crm.clinical.budget.domain.BudgetStatus;
import com.becommerce.crm.clinical.procedure.application.dto.ProcedureToothDto;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(UUID id, UUID contactId, String title, BudgetStatus status, long subtotalCents,
                             long discountCents, long totalCents, String notes, PaymentMethod paymentMethod,
                             Integer installments, LocalDate firstDueDate, LocalDateTime approvedAt,
                             LocalDateTime lostAt, List<Item> items, List<Installment> installmentsList,
                             LocalDateTime updatedAt, String updatedByName) {

    public record Item(UUID procedureId, String name, List<ProcedureToothDto> teeth, String professionalName,
                       long priceCents) {}

    public record Installment(UUID id, LocalDate dueDate, long amountCents, TransactionStatus status) {}
}
