package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset;
import com.becommerce.crm.reports.export.application.dto.CashFlowDataset;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreGroup;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.AppointmentRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CategoryTotalRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.DailyTotalRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportDatasetBuilderTest {

    @Test
    void dreGroupsCategoriesAndSubtractsExpensesAndFees() {
        MonthlyDreDataset dre = ReportDatasetBuilder.dre(List.of(
                new CategoryTotalRow(TransactionCategory.PROCEDURE, 3, 300_000, 1_500, 2),
                new CategoryTotalRow(TransactionCategory.CONSULTATION, 2, 50_000, 0, 0),
                new CategoryTotalRow(TransactionCategory.TAXES, 1, 20_000, 0, 0),
                new CategoryTotalRow(TransactionCategory.RENT, 1, 100_000, 0, 0),
                new CategoryTotalRow(TransactionCategory.SUPPLIES, 2, 10_000, 0, 0)));

        assertThat(dre.groups()).extracting(DreGroup::label).containsExactly("Receitas",
                "Deduções sobre receita", "Despesas com pessoal", "Despesas administrativas",
                "Divulgação e marketing", "Despesas clínicas", "Resultado financeiro");

        DreGroup revenue = dre.groups().get(0);
        assertThat(revenue.lines()).hasSize(4);
        assertThat(revenue.totalCents()).isEqualTo(350_000);
        assertThat(revenue.count()).isEqualTo(5);

        DreGroup admin = dre.groups().get(3);
        assertThat(admin.lines()).hasSize(4);
        assertThat(admin.totalCents()).isEqualTo(100_000);

        assertThat(dre.groups().get(2).totalCents()).isZero();
        assertThat(dre.groups().get(6).totalCents()).isEqualTo(1_500);
        assertThat(dre.groups().get(6).count()).isEqualTo(2);

        // 350.000 - 20.000 - 100.000 - 10.000 - 1.500
        assertThat(dre.resultCents()).isEqualTo(218_500);
    }

    @Test
    void cashFlowAccumulatesByDay() {
        LocalDate d1 = LocalDate.of(2026, 6, 1);
        LocalDate d2 = LocalDate.of(2026, 6, 2);
        CashFlowDataset flow = ReportDatasetBuilder.cashFlow(List.of(
                new DailyTotalRow(d2, TransactionType.EXPENSE, 5_000),
                new DailyTotalRow(d1, TransactionType.INCOME, 10_000),
                new DailyTotalRow(d1, TransactionType.EXPENSE, 2_000)));

        assertThat(flow.days()).hasSize(2);
        assertThat(flow.days().get(0).date()).isEqualTo(d1);
        assertThat(flow.days().get(0).cumulativeCents()).isEqualTo(8_000);
        assertThat(flow.days().get(1).cumulativeCents()).isEqualTo(3_000);
        assertThat(flow.balanceCents()).isEqualTo(3_000);
    }

    @Test
    void agendaCountsStatusPerProfessional() {
        Instant now = Instant.now();
        AgendaDataset agenda = ReportDatasetBuilder.agenda(List.of(
                new AppointmentRow(now, "Ana", "Consulta", "Dra. Bia", "COMPLETED", "INTERNAL"),
                new AppointmentRow(now, "Caio", "Consulta", "Dra. Bia", "CANCELED", "WHATSAPP"),
                new AppointmentRow(now, "Duda", "Retorno", "", "SCHEDULED", "INTERNAL")));

        assertThat(agenda.professionals()).hasSize(2);
        assertThat(agenda.professionals().get(0).completed()).isEqualTo(1);
        assertThat(agenda.professionals().get(0).canceled()).isEqualTo(1);
        assertThat(agenda.professionals().get(1).name()).isEqualTo("Sem profissional");
        assertThat(agenda.professionals().get(1).total()).isEqualTo(1);
    }
}
