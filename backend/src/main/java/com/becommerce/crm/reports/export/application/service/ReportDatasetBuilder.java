package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.finance.accounting.application.service.FinanceLabels;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset.ProfessionalSummary;
import com.becommerce.crm.reports.export.application.dto.CashFlowDataset;
import com.becommerce.crm.reports.export.application.dto.CashFlowDataset.CashFlowDay;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreGroup;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreLine;
import com.becommerce.crm.reports.export.application.dto.TransactionsDataset;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.AppointmentRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CategoryTotalRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.DailyTotalRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.TransactionRow;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Monta os dados de cada relatório a partir do {@link ReportDataGateway}. As
 * regras (agrupamento da DRE, saldo acumulado, contagem por status) ficam em
 * métodos estáticos puros para teste sem banco.
 */
@Component
public class ReportDatasetBuilder {

    /** Grupos da DRE e as categorias de lançamento que compõem cada um. */
    private static final List<GroupSpec> DRE_GROUPS = List.of(
            new GroupSpec("Receitas", true, List.of(TransactionCategory.PROCEDURE,
                    TransactionCategory.CONSULTATION, TransactionCategory.PRODUCT_SALE,
                    TransactionCategory.OTHER_INCOME)),
            new GroupSpec("Deduções sobre receita", false, List.of(TransactionCategory.TAXES)),
            new GroupSpec("Despesas com pessoal", false, List.of(TransactionCategory.PAYROLL)),
            new GroupSpec("Despesas administrativas", false, List.of(TransactionCategory.RENT,
                    TransactionCategory.UTILITIES, TransactionCategory.EQUIPMENT,
                    TransactionCategory.OTHER_EXPENSE)),
            new GroupSpec("Divulgação e marketing", false, List.of(TransactionCategory.MARKETING)),
            new GroupSpec("Despesas clínicas", false, List.of(TransactionCategory.SUPPLIES,
                    TransactionCategory.LAB)));

    static final String FINANCIAL_GROUP = "Resultado financeiro";
    static final String CARD_FEES = "Taxas de cartão/maquininha";

    private final ReportDataGateway gateway;

    public ReportDatasetBuilder(ReportDataGateway gateway) {
        this.gateway = gateway;
    }

    public MonthlyDreDataset monthlyDre(UUID companyId, LocalDate from, LocalDate to) {
        return dre(gateway.paidByCategory(companyId, from, to));
    }

    public CashFlowDataset cashFlow(UUID companyId, LocalDate from, LocalDate to) {
        return cashFlow(gateway.paidByDay(companyId, from, to));
    }

    public TransactionsDataset transactions(UUID companyId, LocalDate from, LocalDate to) {
        return transactions(gateway.transactions(companyId, from, to));
    }

    public AgendaDataset agenda(UUID companyId, LocalDate from, LocalDate to) {
        return agenda(gateway.appointments(companyId, from, to));
    }

    static MonthlyDreDataset dre(List<CategoryTotalRow> rows) {
        Map<TransactionCategory, CategoryTotalRow> byCategory = new EnumMap<>(TransactionCategory.class);
        long feeCents = 0L;
        long feeCount = 0L;
        for (CategoryTotalRow row : rows) {
            byCategory.put(row.category(), row);
            feeCents += row.feeCents();
            if (row.feeCents() > 0) {
                feeCount += row.count();
            }
        }
        List<DreGroup> groups = new ArrayList<>();
        long result = 0L;
        for (GroupSpec spec : DRE_GROUPS) {
            List<DreLine> lines = new ArrayList<>();
            for (TransactionCategory category : spec.categories()) {
                CategoryTotalRow row = byCategory.get(category);
                lines.add(new DreLine(FinanceLabels.categoryLabel(category),
                        row == null ? 0L : row.count(), row == null ? 0L : row.totalCents()));
            }
            DreGroup group = new DreGroup(spec.label(), spec.revenue(), lines);
            result += spec.revenue() ? group.totalCents() : -group.totalCents();
            groups.add(group);
        }
        groups.add(new DreGroup(FINANCIAL_GROUP, false, List.of(new DreLine(CARD_FEES, feeCount, feeCents))));
        result -= feeCents;
        return new MonthlyDreDataset(groups, result);
    }

    static CashFlowDataset cashFlow(List<DailyTotalRow> rows) {
        Map<LocalDate, long[]> byDay = new TreeMap<>();
        for (DailyTotalRow row : rows) {
            long[] totals = byDay.computeIfAbsent(row.date(), d -> new long[2]);
            totals[row.type() == TransactionType.INCOME ? 0 : 1] += row.totalCents();
        }
        List<CashFlowDay> days = new ArrayList<>();
        long cumulative = 0L;
        long income = 0L;
        long expense = 0L;
        for (Map.Entry<LocalDate, long[]> entry : byDay.entrySet()) {
            long[] totals = entry.getValue();
            cumulative += totals[0] - totals[1];
            income += totals[0];
            expense += totals[1];
            days.add(new CashFlowDay(entry.getKey(), totals[0], totals[1], cumulative));
        }
        return new CashFlowDataset(days, income, expense);
    }

    static TransactionsDataset transactions(List<TransactionRow> rows) {
        long income = 0L;
        long expense = 0L;
        for (TransactionRow row : rows) {
            if (row.type() == TransactionType.INCOME) {
                income += row.amountCents();
            } else {
                expense += row.amountCents();
            }
        }
        return new TransactionsDataset(rows, income, expense);
    }

    static AgendaDataset agenda(List<AppointmentRow> rows) {
        Map<String, long[]> byHost = new LinkedHashMap<>();
        for (AppointmentRow row : rows) {
            String host = row.hostName() == null || row.hostName().isBlank() ? "Sem profissional" : row.hostName();
            long[] counts = byHost.computeIfAbsent(host, h -> new long[5]);
            switch (row.status()) {
                case "SCHEDULED" -> counts[0]++;
                case "CONFIRMED" -> counts[1]++;
                case "COMPLETED" -> counts[2]++;
                case "NO_SHOW" -> counts[3]++;
                case "CANCELED" -> counts[4]++;
                default -> { }
            }
        }
        List<ProfessionalSummary> professionals = new ArrayList<>();
        byHost.forEach((name, c) -> professionals.add(new ProfessionalSummary(name, c[0], c[1], c[2], c[3], c[4])));
        return new AgendaDataset(professionals, rows);
    }

    private record GroupSpec(String label, boolean revenue, List<TransactionCategory> categories) {}
}
