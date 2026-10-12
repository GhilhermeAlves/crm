package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.tool.AbstractAiReadTool;
import com.becommerce.crm.automation.ai.application.tool.AgendaFormat;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.MoneyFormat;
import com.becommerce.crm.finance.report.application.dto.SummaryResponse;
import com.becommerce.crm.finance.report.application.service.FinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * {@code finance_summary}: resumo financeiro do período (receitas e despesas
 * pagas e em aberto, e o saldo realizado). Reutiliza
 * {@link FinanceReportService#summary}. Datas em {@code AAAA-MM-DD} (padrão:
 * mês corrente). Exige {@code finance:read}.
 */
@Component
public class FinanceSummaryTool extends AbstractAiReadTool {

    private static final String NAME = "finance_summary";

    private final FinanceReportService reportService;
    private final Clock clock;

    @Autowired
    public FinanceSummaryTool(FinanceReportService reportService) {
        this(reportService, Clock.system(AgendaFormat.CLINIC_ZONE));
    }

    FinanceSummaryTool(FinanceReportService reportService, Clock clock) {
        super(NAME, "Resumo financeiro do período: total de receitas e despesas pagas, valores em "
                        + "aberto e o saldo realizado. Datas no formato AAAA-MM-DD (padrão: mês atual).",
                "finance:read",
                Map.of("from", stringProp("Data inicial AAAA-MM-DD (padrão: primeiro dia do mês)"),
                        "to", stringProp("Data final AAAA-MM-DD (padrão: último dia do mês)")));
        this.reportService = reportService;
        this.clock = clock;
    }

    public record FinanceSummary(String receitasPagas, String receitasAbertas, String despesasPagas,
                                 String despesasAbertas, String saldoRealizado) {
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        LocalDate today = LocalDate.now(clock.withZone(AgendaFormat.CLINIC_ZONE));
        LocalDate from = parseDate(string(arguments, "from"), today.withDayOfMonth(1));
        LocalDate to = parseDate(string(arguments, "to"), today.withDayOfMonth(today.lengthOfMonth()));
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à final.");
        }
        SummaryResponse summary = reportService.summary(ctx.companyId(), null, null, from, to, null, null,
                null, null, null);
        return AiToolResult.ok(NAME, new FinanceSummary(
                MoneyFormat.brl(summary.incomeTotalCents()),
                MoneyFormat.brl(summary.incomeForecastCents()),
                MoneyFormat.brl(summary.expenseTotalCents()),
                MoneyFormat.brl(summary.expenseForecastCents()),
                MoneyFormat.brl(summary.balanceCents())));
    }

    private LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: use o formato AAAA-MM-DD.");
        }
    }
}
