package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.finance.accounting.application.service.FinanceLabels;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset.ProfessionalSummary;
import com.becommerce.crm.reports.export.application.dto.CashFlowDataset;
import com.becommerce.crm.reports.export.application.dto.CashFlowDataset.CashFlowDay;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreGroup;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreLine;
import com.becommerce.crm.reports.export.application.dto.TransactionsDataset;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.AppointmentRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.TransactionRow;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Column;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Money;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ReportTable;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Row;
import com.becommerce.crm.reports.export.domain.ReportExportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Converte o dataset de cada tipo em um {@link ReportDocument} — o layout de
 * cada relatório é descrito uma vez aqui e reaproveitado pelo PDF e pelo Excel.
 */
public final class ReportDocumentFactory {

    public static final String RESULT_LABEL = "Resultado do mês";

    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReportDocumentFactory() {
    }

    public static ReportDocument monthlyDre(MonthlyDreDataset data, YearMonth month, CompanyHeader company,
                                            LocalDateTime issuedAt) {
        List<Row> rows = new ArrayList<>();
        for (DreGroup group : data.groups()) {
            rows.add(Row.group(group.label()));
            for (DreLine line : group.lines()) {
                rows.add(Row.item(line.label(), line.count(), new Money(signed(group, line.totalCents()))));
            }
            rows.add(Row.subtotal("Total " + lowerFirst(group.label()), group.count(),
                    new Money(signed(group, group.totalCents()))));
        }
        rows.add(Row.total(RESULT_LABEL, null, new Money(data.resultCents())));
        ReportTable table = new ReportTable(null, List.of(
                Column.text("Conta", 4f), Column.count("Lançamentos"), Column.money("Valor")), rows);
        return new ReportDocument(ReportExportType.MONTHLY_DRE.label() + " — regime de caixa",
                FinanceLabels.monthLabel(month), company.name(), company.cnpj(), issuedAt, false, List.of(table));
    }

    public static ReportDocument cashFlow(CashFlowDataset data, LocalDate from, LocalDate to,
                                          CompanyHeader company, LocalDateTime issuedAt) {
        List<Row> rows = new ArrayList<>();
        for (CashFlowDay day : data.days()) {
            rows.add(Row.item(day.date(), new Money(day.incomeCents()), new Money(-day.expenseCents()),
                    new Money(day.balanceCents()), new Money(day.cumulativeCents())));
        }
        rows.add(Row.total("Total do período", new Money(data.incomeCents()), new Money(-data.expenseCents()),
                new Money(data.balanceCents()), new Money(data.balanceCents())));
        ReportTable table = new ReportTable(null, List.of(Column.date("Data"), Column.money("Entradas"),
                Column.money("Saídas"), Column.money("Saldo do dia"), Column.money("Saldo acumulado")), rows);
        return new ReportDocument(ReportExportType.CASH_FLOW.label() + " — realizado", range(from, to),
                company.name(), company.cnpj(), issuedAt, false, List.of(table));
    }

    public static ReportDocument transactions(TransactionsDataset data, LocalDate from, LocalDate to,
                                              CompanyHeader company, LocalDateTime issuedAt) {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        List<Row> rows = new ArrayList<>();
        for (TransactionRow row : data.rows()) {
            long signedCents = row.type() == TransactionType.EXPENSE ? -row.amountCents() : row.amountCents();
            boolean overdue = row.dueDate().isBefore(today);
            rows.add(Row.item(row.dueDate(), row.paidAt(), row.description(), blank(row.contactName()),
                    FinanceLabels.categoryLabel(row.category()), FinanceLabels.paymentLabel(row.paymentMethod()),
                    row.type() == TransactionType.INCOME ? "Receita" : "Despesa", new Money(signedCents),
                    FinanceLabels.statusLabel(row.status(), overdue)));
        }
        rows.add(Row.subtotal("Total de receitas", null, null, null, null, null, null,
                new Money(data.incomeCents()), null));
        rows.add(Row.subtotal("Total de despesas", null, null, null, null, null, null,
                new Money(-data.expenseCents()), null));
        rows.add(Row.total("Saldo do período", null, null, null, null, null, null,
                new Money(data.balanceCents()), null));
        ReportTable table = new ReportTable(null, List.of(Column.date("Vencimento"), Column.date("Pagamento"),
                Column.text("Descrição", 3f), Column.text("Paciente", 2f), Column.text("Categoria", 1.6f),
                Column.text("Forma de pagamento", 1.5f), Column.text("Tipo", 0.9f), Column.money("Valor"),
                Column.text("Status", 1f)), rows);
        return new ReportDocument(ReportExportType.TRANSACTIONS.label(), range(from, to), company.name(),
                company.cnpj(), issuedAt, true, List.of(table));
    }

    public static ReportDocument agenda(AgendaDataset data, LocalDate from, LocalDate to, CompanyHeader company,
                                        LocalDateTime issuedAt) {
        List<Row> summaryRows = new ArrayList<>();
        long[] totals = new long[6];
        for (ProfessionalSummary p : data.professionals()) {
            summaryRows.add(Row.item(p.name(), p.scheduled(), p.confirmed(), p.completed(), p.noShow(),
                    p.canceled(), p.total()));
            totals[0] += p.scheduled();
            totals[1] += p.confirmed();
            totals[2] += p.completed();
            totals[3] += p.noShow();
            totals[4] += p.canceled();
            totals[5] += p.total();
        }
        summaryRows.add(Row.total("Total", totals[0], totals[1], totals[2], totals[3], totals[4], totals[5]));
        ReportTable summary = new ReportTable("Resumo por profissional", List.of(Column.text("Profissional", 3f),
                Column.count("Agendadas"), Column.count("Confirmadas"), Column.count("Realizadas"),
                Column.count("Faltas"), Column.count("Canceladas"), Column.count("Total")), summaryRows);

        List<Row> detailRows = new ArrayList<>();
        for (AppointmentRow row : data.rows()) {
            detailRows.add(Row.item(DATE_TIME.format(row.startAt().atZone(CLINIC_ZONE)),
                    blank(row.contactName()), blank(row.typeName()), blank(row.hostName()),
                    statusLabel(row.status()), sourceLabel(row.source())));
        }
        ReportTable detail = new ReportTable("Consultas", List.of(Column.text("Data e hora", 1.4f),
                Column.text("Paciente", 2.4f), Column.text("Tipo", 2f), Column.text("Profissional", 2f),
                Column.text("Status", 1.2f), Column.text("Origem", 1.1f)), detailRows);
        return new ReportDocument(ReportExportType.AGENDA.label(), range(from, to), company.name(),
                company.cnpj(), issuedAt, true, List.of(summary, detail));
    }

    static String statusLabel(String status) {
        return switch (status) {
            case "SCHEDULED" -> "Agendada";
            case "CONFIRMED" -> "Confirmada";
            case "COMPLETED" -> "Realizada";
            case "NO_SHOW" -> "Falta";
            case "CANCELED" -> "Cancelada";
            default -> status;
        };
    }

    static String sourceLabel(String source) {
        return switch (source) {
            case "INTERNAL" -> "CRM";
            case "PUBLIC_LINK" -> "Link público";
            case "WHATSAPP" -> "WhatsApp";
            default -> source;
        };
    }

    private static long signed(DreGroup group, long cents) {
        return group.revenue() ? cents : -cents;
    }

    private static String range(LocalDate from, LocalDate to) {
        return FinanceLabels.dateLabel(from) + " a " + FinanceLabels.dateLabel(to);
    }

    private static String lowerFirst(String text) {
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
