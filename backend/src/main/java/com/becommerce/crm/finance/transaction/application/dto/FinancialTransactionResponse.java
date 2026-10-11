package com.becommerce.crm.finance.transaction.application.dto;

import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lançamento para a API. Traz os nomes resolvidos (contato, profissional e item
 * de catálogo) e o campo calculado {@code overdue} ("atrasado" na leitura).
 */
public record FinancialTransactionResponse(
        UUID id,
        UUID companyId,
        TransactionType type,
        String description,
        TransactionCategory category,
        long amountCents,
        long feeCents,
        PaymentMethod paymentMethod,
        LocalDate dueDate,
        LocalDate paidAt,
        TransactionStatus status,
        UUID contactId,
        String contactName,
        UUID professionalUserId,
        String professionalName,
        UUID catalogItemId,
        String catalogItemName,
        UUID installmentGroupId,
        Integer installmentNumber,
        Integer installmentTotal,
        String notes,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean overdue
) {
    public static FinancialTransactionResponse from(FinancialTransaction transaction,
                                                    FinancialTransactionRepository.TransactionReference reference,
                                                    LocalDate today) {
        return new FinancialTransactionResponse(
                transaction.getId(), transaction.getCompanyId(), transaction.getType(),
                transaction.getDescription(), transaction.getCategory(), transaction.getAmountCents(),
                transaction.getFeeCents(), transaction.getPaymentMethod(), transaction.getDueDate(),
                transaction.getPaidAt(), transaction.getStatus(), transaction.getContactId(),
                reference == null ? null : reference.contactName(), transaction.getProfessionalUserId(),
                reference == null ? null : reference.professionalName(), transaction.getCatalogItemId(),
                reference == null ? null : reference.catalogItemName(), transaction.getInstallmentGroupId(),
                transaction.getInstallmentNumber(), transaction.getInstallmentTotal(), transaction.getNotes(),
                transaction.getCreatedBy(), transaction.getCreatedAt(), transaction.getUpdatedAt(),
                transaction.isOverdue(today));
    }
}
