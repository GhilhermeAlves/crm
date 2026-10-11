"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

import { useCashFlow } from "../../hooks/useFinance";
import type { ReportGranularity } from "../../types/finance.types";
import { CashFlowChart, CASH_FLOW_COLORS, type CashFlowPoint } from "./CashFlowChart";
import { ReportCard, type ReportLegendItem } from "./ReportCard";
import { ReportGranularityChip } from "./ReportGranularityChip";
import { ReportLineChart, type ReportLineSeries } from "./ReportLineChart";
import { ReportPeriodChip } from "./ReportPeriodChip";
import { ReportPieChart, type ReportPieSlice } from "./ReportPieChart";
import { ReportShell } from "./ReportShell";
import { downloadCsv, type CsvValue } from "./report-csv";
import { formatCompactMoney, formatMoneyLabel, formatPeriodTick } from "./report-format";
import { toPaymentMethodValues } from "./report-cash-flow";
import {
  reportPeriodLabel,
  resolveReportPeriod,
  type ReportPeriodKey,
  type ReportPeriodRange,
} from "./report-period";

const INCOME_HISTORY_SERIES: ReportLineSeries[] = [
  { key: "forecast", name: "Previsão", color: "#eab308" },
  { key: "income", name: "Entrada", color: "#16a34a" },
  { key: "default", name: "Inadimplência", color: "#dc2626" },
  { key: "open", name: "Em Aberto", color: "#2563eb" },
];

const EXPENSE_HISTORY_SERIES: ReportLineSeries[] = [
  { key: "expense", name: "Saída", color: "#dc2626" },
  { key: "open", name: "Em Aberto", color: "#2563eb" },
  { key: "default", name: "Inadimplência", color: "#f59e0b" },
];

const CASH_FLOW_LEGEND: ReportLegendItem[] = [
  { label: "Entradas", color: CASH_FLOW_COLORS.income },
  { label: "Recebimentos em Aberto", color: CASH_FLOW_COLORS.openReceivables },
  { label: "Saídas", color: CASH_FLOW_COLORS.expense },
  { label: "Pagamentos em Aberto", color: CASH_FLOW_COLORS.openPayables },
  { label: "Saldo Mensal", color: CASH_FLOW_COLORS.periodBalance },
  { label: "Saldo Acumulado", color: CASH_FLOW_COLORS.accumulatedBalance },
];

function CashFlowSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-80 w-full" />
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-8">
        {Array.from({ length: 8 }).map((_, index) => (
          <Skeleton key={index} className="h-20" />
        ))}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Skeleton className="h-72" />
        <Skeleton className="h-72" />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Skeleton className="h-72" />
        <Skeleton className="h-72" />
      </div>
    </div>
  );
}

type CashFlowReportProps = {
  companyId: string | null;
};

export function CashFlowReport({ companyId }: CashFlowReportProps) {
  const [period, setPeriod] = useState<ReportPeriodKey>("last-2-months");
  const [customRange, setCustomRange] = useState<ReportPeriodRange | null>(null);
  const [granularity, setGranularity] = useState<ReportGranularity>("MONTH");

  const range = resolveReportPeriod(period, customRange);
  const periodLabel = reportPeriodLabel(period, customRange);

  const query = useCashFlow(companyId, {
    from: range.from,
    to: range.to,
    granularity,
  });

  const data = query.data;

  const seriesData: CashFlowPoint[] = (data?.series ?? []).map((item) => ({
    period: formatPeriodTick(item.period, granularity),
    income: item.incomeCents / 100,
    openReceivables: item.openReceivablesCents / 100,
    expense: item.expenseCents / 100,
    openPayables: item.openPayablesCents / 100,
    periodBalance: item.periodBalanceCents / 100,
    accumulatedBalance: item.accumulatedBalanceCents / 100,
  }));

  const incomeHistoryData = (data?.incomeHistory ?? []).map((item) => ({
    period: formatPeriodTick(item.period, granularity),
    forecast: item.forecastCents / 100,
    income: item.incomeCents / 100,
    default: item.defaultCents / 100,
    open: item.openCents / 100,
  }));

  const expenseHistoryData = (data?.expenseHistory ?? []).map((item) => ({
    period: formatPeriodTick(item.period, granularity),
    expense: item.expenseCents / 100,
    open: item.openCents / 100,
    default: item.defaultCents / 100,
  }));

  const methodValues = toPaymentMethodValues(data?.incomeByMethod);

  const incomeMonth = data?.currentMonth.income;
  const expenseMonth = data?.currentMonth.expense;

  const incomePie: ReportPieSlice[] = [
    { name: "Entrada", value: (incomeMonth?.paidCents ?? 0) / 100, color: "#16a34a" },
    { name: "Em Aberto", value: (incomeMonth?.openCents ?? 0) / 100, color: "#2563eb" },
    { name: "Inadimplência", value: (incomeMonth?.defaultCents ?? 0) / 100, color: "#dc2626" },
  ];
  const expensePie: ReportPieSlice[] = [
    { name: "Saída", value: (expenseMonth?.paidCents ?? 0) / 100, color: "#dc2626" },
    { name: "Em Aberto", value: (expenseMonth?.openCents ?? 0) / 100, color: "#2563eb" },
    { name: "Inadimplência", value: (expenseMonth?.defaultCents ?? 0) / 100, color: "#f59e0b" },
  ];

  const handlePeriodChange = (next: ReportPeriodKey, nextCustom?: ReportPeriodRange) => {
    setPeriod(next);
    setCustomRange(next === "custom" ? (nextCustom ?? null) : null);
  };

  const handleExport = () => {
    const rows: CsvValue[][] = [
      ["Relatório de Fluxo de Caixa"],
      ["Período", periodLabel],
      [],
      ["Fluxo de Caixa"],
      [
        "Período",
        "Entradas (R$)",
        "Recebimentos em Aberto (R$)",
        "Saídas (R$)",
        "Pagamentos em Aberto (R$)",
        "Saldo Mensal (R$)",
        "Saldo Acumulado (R$)",
      ],
      ...(data?.series ?? []).map((item) => [
        item.period,
        (item.incomeCents / 100).toFixed(2),
        (item.openReceivablesCents / 100).toFixed(2),
        (item.expenseCents / 100).toFixed(2),
        (item.openPayablesCents / 100).toFixed(2),
        (item.periodBalanceCents / 100).toFixed(2),
        (item.accumulatedBalanceCents / 100).toFixed(2),
      ]),
      [],
      ["Detalhamento de Entradas"],
      ["Forma de Pagamento", "Valor (R$)"],
      ...methodValues.map((item) => [item.label, (item.cents / 100).toFixed(2)]),
      [],
      ["Histórico de Entrada, Previsão e Inadimplência"],
      ["Período", "Previsão (R$)", "Entrada (R$)", "Inadimplência (R$)", "Em Aberto (R$)"],
      ...(data?.incomeHistory ?? []).map((item) => [
        item.period,
        (item.forecastCents / 100).toFixed(2),
        (item.incomeCents / 100).toFixed(2),
        (item.defaultCents / 100).toFixed(2),
        (item.openCents / 100).toFixed(2),
      ]),
      [],
      ["Detalhamento de Saídas"],
      ["Histórico de Saída, Previsão e Inadimplência"],
      ["Período", "Saída (R$)", "Em Aberto (R$)", "Inadimplência (R$)"],
      ...(data?.expenseHistory ?? []).map((item) => [
        item.period,
        (item.expenseCents / 100).toFixed(2),
        (item.openCents / 100).toFixed(2),
        (item.defaultCents / 100).toFixed(2),
      ]),
    ];
    downloadCsv("relatorio-fluxo-de-caixa.csv", rows);
  };

  return (
    <ReportShell
      title="Relatório de Fluxo de Caixa"
      subtitle="Confira entradas, saídas, valores em aberto, previsões de recebimentos e pagamentos."
      panelTitle="Fluxo de Caixa"
      onExport={handleExport}
    >
      <div className="flex flex-wrap items-center gap-2">
        <ReportPeriodChip period={period} custom={customRange} onChange={handlePeriodChange} />
        <ReportGranularityChip value={granularity} onChange={setGranularity} />
      </div>

      {query.isLoading ? (
        <CashFlowSkeleton />
      ) : query.isError ? (
        <Card className="flex flex-col items-center gap-3 py-12 text-muted-foreground">
          <p className="text-sm">Não foi possível carregar o relatório.</p>
          <Button variant="outline" size="sm" onClick={() => query.refetch()}>
            Tentar novamente
          </Button>
        </Card>
      ) : (
        <div className="space-y-6">
          <ReportCard
            title="Fluxo de Caixa"
            legend={CASH_FLOW_LEGEND}
            isEmpty={seriesData.length === 0}
          >
            <CashFlowChart
              data={seriesData}
              tickFormatter={(value) => value}
              valueFormatter={formatCompactMoney}
              labelFormatter={formatMoneyLabel}
            />
          </ReportCard>

          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Detalhamento de Entradas</h3>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-8">
              {methodValues.map((item) => (
                <Card key={item.label} className="p-3">
                  <p className="truncate text-xs text-muted-foreground" title={item.label}>
                    {item.label}
                  </p>
                  <p className="mt-2 text-center text-base font-semibold tracking-tight">
                    {formatCompactMoney(item.cents / 100)}
                  </p>
                </Card>
              ))}
            </div>
            <div className="grid gap-4 lg:grid-cols-2">
              <ReportCard
                title="Histórico de Entrada, Previsão e Inadimplência"
                legend={INCOME_HISTORY_SERIES.map(({ name, color }) => ({ label: name, color }))}
                isEmpty={incomeHistoryData.length === 0}
              >
                <ReportLineChart
                  data={incomeHistoryData}
                  xKey="period"
                  series={INCOME_HISTORY_SERIES}
                  valueFormatter={formatCompactMoney}
                />
              </ReportCard>
              <ReportCard
                title="Entrada, Em Aberto e Inadimplência - Mês Atual"
                legend={incomePie.map(({ name, color }) => ({ label: name, color }))}
                isEmpty={incomePie.every((slice) => slice.value === 0)}
              >
                <ReportPieChart data={incomePie} valueFormatter={formatCompactMoney} />
              </ReportCard>
            </div>
          </section>

          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Detalhamento de Saídas</h3>
            <div className="grid gap-4 lg:grid-cols-2">
              <ReportCard
                title="Histórico de Saída, Previsão e Inadimplência"
                legend={EXPENSE_HISTORY_SERIES.map(({ name, color }) => ({ label: name, color }))}
                isEmpty={expenseHistoryData.length === 0}
              >
                <ReportLineChart
                  data={expenseHistoryData}
                  xKey="period"
                  series={EXPENSE_HISTORY_SERIES}
                  valueFormatter={formatCompactMoney}
                />
              </ReportCard>
              <ReportCard
                title="Saída, Em Aberto e Inadimplência - Mês Atual"
                legend={expensePie.map(({ name, color }) => ({ label: name, color }))}
                isEmpty={expensePie.every((slice) => slice.value === 0)}
              >
                <ReportPieChart data={expensePie} valueFormatter={formatCompactMoney} />
              </ReportCard>
            </div>
          </section>
        </div>
      )}
    </ReportShell>
  );
}
