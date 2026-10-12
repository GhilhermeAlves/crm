package com.becommerce.crm.reports.export.application.port.out;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Leituras (somente SELECT) que alimentam os relatórios exportados. Filtros
 * sempre preenchidos — nada de {@code :x IS NULL OR ...}, que quebra no
 * PostgreSQL por falta de tipo do parâmetro.
 */
public interface ReportDataGateway {

    /** Nome fantasia (ou razão social) e CNPJ da empresa, para o cabeçalho. */
    CompanyHeader companyHeader(UUID companyId);

    /** Lançamentos PAID com {@code paid_at} no período, somados por categoria. */
    List<CategoryTotalRow> paidByCategory(UUID companyId, LocalDate from, LocalDate to);

    /** Lançamentos PAID com {@code paid_at} no período, somados por dia e tipo. */
    List<DailyTotalRow> paidByDay(UUID companyId, LocalDate from, LocalDate to);

    /** Lançamentos não cancelados com vencimento no período. */
    List<TransactionRow> transactions(UUID companyId, LocalDate from, LocalDate to);

    /** Consultas com início (horário de Brasília) no período. */
    List<AppointmentRow> appointments(UUID companyId, LocalDate from, LocalDate to);

    record CompanyHeader(String name, String cnpj) {}

    record CategoryTotalRow(TransactionCategory category, long count, long totalCents, long feeCents) {}

    record DailyTotalRow(LocalDate date, TransactionType type, long totalCents) {}

    record TransactionRow(LocalDate dueDate, LocalDate paidAt, String description, String contactName,
                          TransactionCategory category, PaymentMethod paymentMethod, TransactionType type,
                          long amountCents, TransactionStatus status) {}

    record AppointmentRow(Instant startAt, String contactName, String typeName, String hostName,
                          String status, String source) {}
}
