package com.becommerce.crm.finance.accounting.application.port.output;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Porta de saída dos dados dos relatórios contábeis e da importação CSV
 * (Etapa 3). SELECTs nativos sobre {@code financial_transactions},
 * {@code contacts}, {@code users} e {@code companies} — o isolamento por tenant
 * é o RLS FORCE (V092) via {@code TenantAwareDataSource} + {@code company_id}.
 */
public interface AccountingReportGateway {

    /** Nome comercial da empresa (ou razão social) para o cabeçalho do PDF. */
    String companyName(UUID companyId);

    /** Receitas e despesas PAID do mês (paid_at), somadas por categoria. */
    List<CategoryTotalRow> monthlyAccounting(UUID companyId, LocalDate monthStart, LocalDate monthEnd);

    /** Lançamentos PAID do mês (paid_at) na ordem do extrato. */
    List<StatementRow> statement(UUID companyId, LocalDate monthStart, LocalDate monthEnd);

    /** Lançamentos do mês por due_date (exceto CANCELLED). */
    List<TransactionDetailRow> transactionDetails(UUID companyId, LocalDate monthStart, LocalDate monthEnd);

    /** Meses (yyyy-MM) com algum lançamento não cancelado, do mais recente ao mais antigo. */
    List<String> availableMonths(UUID companyId);

    /** Contato da empresa pelo nome completo exato (case-insensitive); {@code null} se não achar. */
    UUID resolveContact(UUID companyId, String fullName);

    /** Membro da empresa (usuário) por nome ou e-mail exato (case-insensitive); {@code null} se não achar. */
    UUID resolveProfessional(UUID companyId, String nameOrEmail);

    record CategoryTotalRow(TransactionType type, TransactionCategory category, long totalCents,
                            long feeCents) {}

    record StatementRow(LocalDate paidAt, String description, TransactionCategory category,
                        TransactionType type, long amountCents) {}

    record TransactionDetailRow(LocalDate dueDate, LocalDate paidAt, String description,
                                TransactionCategory category, TransactionType type, long amountCents,
                                long feeCents, PaymentMethod paymentMethod, TransactionStatus status,
                                Integer installmentNumber, Integer installmentTotal, String contactName,
                                String professionalName) {}
}