package com.becommerce.crm.finance.report.application.dto;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtros do /summary, espelhando os da listagem da etapa 1. O período é aplicado
 * a {@code paid_at} (valores PAID) e a {@code due_date} (valores PENDING).
 * {@code status} aceita PENDING/PAID/CANCELLED ou o calculado OVERDUE (que é
 * normalizado para PENDING + vencimento anterior a hoje).
 */
public record SummaryFilter(
        TransactionType type,
        String status,
        LocalDate overdueBefore,
        LocalDate from,
        LocalDate to,
        PaymentMethod paymentMethod,
        TransactionCategory category,
        UUID professionalUserId,
        UUID contactId,
        String pattern) {}