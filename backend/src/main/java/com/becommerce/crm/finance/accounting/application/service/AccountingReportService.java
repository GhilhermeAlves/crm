package com.becommerce.crm.finance.accounting.application.service;

import com.becommerce.crm.finance.accounting.application.dto.AccountingReportDataset;
import com.becommerce.crm.finance.accounting.application.dto.AccountingReportRequestDto;
import com.becommerce.crm.finance.accounting.application.dto.MonthlyAccountingData;
import com.becommerce.crm.finance.accounting.application.dto.MonthlyAccountingData.MonthlyCategory;
import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportGateway;
import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportRequestRepository;
import com.becommerce.crm.finance.accounting.domain.AccountingReportType;
import com.becommerce.crm.finance.accounting.domain.ReportFormat;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Relatórios contábeis (Etapa 3): monta os dados (100% em centavos) via
 * {@link AccountingReportGateway}, registra a solicitação (V093) e delega a
 * renderização (PDF/CSV) ao {@link AccountingReportRenderer}. Campos do corpo em
 * {@code String} para validar manualmente e responder 400 (evita
 * HttpMessageNotReadableException, que hoje viraria 500).
 */
@Service
public class AccountingReportService {

    private final AccountingReportGateway gateway;
    private final AccountingReportRequestRepository register;
    private final AccountingReportRenderer renderer;

    public AccountingReportService(AccountingReportGateway gateway,
                                   AccountingReportRequestRepository register,
                                   AccountingReportRenderer renderer) {
        this.gateway = gateway;
        this.register = register;
        this.renderer = renderer;
    }

    @Transactional
    public byte[] generate(UUID companyId, AccountingReportRequestDto request, UUID actor) {
        AccountingReportType type = parseType(request.type());
        ReportFormat format = parseFormat(request.format());
        YearMonth month = parseMonth(request.month());
        try {
            TenantContext.setCompanyId(companyId);
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();
            String companyName = gateway.companyName(companyId);
            int generatedCount = register.register(companyId, type, format, month, actor);
            AccountingReportDataset dataset = buildDataset(type, companyId, start, end);
            return renderer.render(type, format, month, companyName, dataset, generatedCount);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public List<String> availableMonths(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return gateway.availableMonths(companyId);
        } finally {
            TenantContext.clear();
        }
    }

    /** Nome do arquivo de download derivado do pedido validado (ex.: relatorio-statement-2026-09.pdf). */
    public String fileName(AccountingReportRequestDto request) {
        AccountingReportType type = parseType(request.type());
        ReportFormat format = parseFormat(request.format());
        YearMonth month = parseMonth(request.month());
        String slug = switch (type) {
            case MONTHLY_ACCOUNTING -> "monthly-accounting";
            case STATEMENT -> "statement";
            case TRANSACTIONS -> "transactions";
        };
        return "relatorio-" + slug + "-" + month + "." + format.name().toLowerCase(Locale.ROOT);
    }

    private AccountingReportDataset buildDataset(AccountingReportType type, UUID companyId, LocalDate start,
                                                 LocalDate end) {
        return switch (type) {
            case MONTHLY_ACCOUNTING -> new AccountingReportDataset(buildMonthly(companyId, start, end),
                    List.of(), List.of());
            case STATEMENT -> new AccountingReportDataset(null,
                    gateway.statement(companyId, start, end), List.of());
            case TRANSACTIONS -> new AccountingReportDataset(null, List.of(),
                    gateway.transactionDetails(companyId, start, end));
        };
    }

    private MonthlyAccountingData buildMonthly(UUID companyId, LocalDate start, LocalDate end) {
        Map<AccountingReportGateway.CategoryTotalRow, Long> totalsByCategory = new LinkedHashMap<>();
        for (AccountingReportGateway.CategoryTotalRow row : gateway.monthlyAccounting(companyId, start, end)) {
            totalsByCategory.merge(row, row.totalCents(), Long::sum);
        }
        List<AccountingReportGateway.CategoryTotalRow> categories = totalsByCategory.keySet().stream()
                .sorted(Comparator.comparing((AccountingReportGateway.CategoryTotalRow row) -> row.type())
                        .thenComparing(row -> row.category()))
                .toList();
        List<MonthlyCategory> incomeCategories = new ArrayList<>();
        List<MonthlyCategory> expensesCategories = new ArrayList<>();
        long incomeTotal = 0L;
        long expenseTotal = 0L;
        long feeTotal = 0L;
        for (AccountingReportGateway.CategoryTotalRow row : categories) {
            if (row.type() == TransactionType.INCOME) {
                incomeCategories.add(new MonthlyCategory(row.category(), totalsByCategory.get(row)));
                incomeTotal += totalsByCategory.get(row);
            } else {
                expensesCategories.add(new MonthlyCategory(row.category(), totalsByCategory.get(row)));
                expenseTotal += totalsByCategory.get(row);
            }
            feeTotal += row.feeCents();
        }
        return new MonthlyAccountingData(incomeCategories, expensesCategories, incomeTotal, expenseTotal,
                feeTotal);
    }

    private static AccountingReportType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new FinancialTransactionValidationException("O tipo do relatório é obrigatório.");
        }
        try {
            return AccountingReportType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new FinancialTransactionValidationException("Tipo de relatório inválido: " + raw + ".");
        }
    }

    private static ReportFormat parseFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new FinancialTransactionValidationException("O formato do relatório é obrigatório.");
        }
        try {
            return ReportFormat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new FinancialTransactionValidationException("Formato inválido: " + raw + ".");
        }
    }

    private static YearMonth parseMonth(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new FinancialTransactionValidationException("O mês do relatório é obrigatório.");
        }
        try {
            return YearMonth.parse(raw.trim(), DateTimeFormatter.ofPattern("uuuu-MM"));
        } catch (DateTimeParseException ex) {
            throw new FinancialTransactionValidationException(
                    "Mês inválido: " + raw + ". Use o formato yyyy-MM.");
        }
    }
}