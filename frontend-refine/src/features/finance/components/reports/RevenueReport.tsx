"use client";

import { useState } from "react";
import { Stethoscope } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCatalog } from "@/features/masterdata/catalog/hooks/useCatalog";

import { useRevenueDistribution } from "../../hooks/useFinance";
import type { ReportGranularity } from "../../types/finance.types";
import { ReportBarChart, type ReportBarSeries } from "./ReportBarChart";
import { ReportCard } from "./ReportCard";
import { ReportGranularityChip } from "./ReportGranularityChip";
import { ReportLineChart } from "./ReportLineChart";
import { ReportListChip } from "./ReportListChip";
import { ReportPeriodChip } from "./ReportPeriodChip";
import { ReportShell } from "./ReportShell";
import { useRequestReportExport } from "@/features/reports/export/useReportExports";
import { formatCompactMoney, formatPeriodTick, truncateLabel } from "./report-format";
import { buildRevenueSeries } from "./report-revenue";
import { resolveReportPeriod, type ReportPeriodKey, type ReportPeriodRange } from "./report-period";

const PAID_NOTE =
  "São considerados somente os valores correspondentes às operações financeiras marcadas como pagas no Controle Financeiro. Exemplo: orçamento composto por um único procedimento de R$200, com parcelamento em 2x de R$100 e vencimento em junho e julho. Se somente a parcela de junho for paga, a receita desse procedimento será de R$100 em junho e R$0 em julho.";

const AVERAGE_SERIES: ReportBarSeries[] = [
  { key: "average", name: "Preço Médio por Procedimento", color: "#16a34a" },
  { key: "averageNet", name: "Preço Médio por Procedimento (com Desconto)", color: "#c4b5fd" },
];

function RevenueSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-72 w-full" />
      <Skeleton className="h-80 w-full" />
      <Skeleton className="h-72 w-full" />
    </div>
  );
}

function PaidNote() {
  return (
    <div className="rounded-md bg-muted px-4 py-3 text-xs text-muted-foreground">{PAID_NOTE}</div>
  );
}

type RevenueReportProps = {
  companyId: string | null;
};

export function RevenueReport({ companyId }: RevenueReportProps) {
  const [period, setPeriod] = useState<ReportPeriodKey>("this-month");
  const [customRange, setCustomRange] = useState<ReportPeriodRange | null>(null);
  const [granularity, setGranularity] = useState<ReportGranularity>("MONTH");
  const [procedureIds, setProcedureIds] = useState<string[]>([]);

  const range = resolveReportPeriod(period, customRange);

  const query = useRevenueDistribution(companyId, {
    from: range.from,
    to: range.to,
    granularity,
    catalogItemIds: procedureIds,
  });
  const catalog = useCatalog(companyId, { type: "SERVICE", active: true, pageSize: 100 });

  const data = query.data;
  const procedureOptions = (catalog.data?.content ?? []).map((item) => ({
    value: item.id,
    label: item.name,
  }));

  const proceduresBuild = buildRevenueSeries(
    (data?.byProcedure ?? []).map((item) => ({
      period: formatPeriodTick(item.period, granularity),
      name: item.name ?? "Sem procedimento",
      amountCents: item.amountCents,
    })),
  );

  const professionalsBuild = buildRevenueSeries(
    (data?.byProfessional ?? []).map((item) => ({
      period: formatPeriodTick(item.period, granularity),
      name: item.name ?? "Sem profissional",
      amountCents: item.amountCents,
    })),
  );

  const averageData = (data?.averageByProcedure ?? []).map((item) => ({
    name: item.name ?? "Sem procedimento",
    average: item.averageCents / 100,
    averageNet: item.averageNetCents / 100,
  }));

  const handlePeriodChange = (next: ReportPeriodKey, nextCustom?: ReportPeriodRange) => {
    setPeriod(next);
    setCustomRange(next === "custom" ? (nextCustom ?? null) : null);
  };

  const requestExport = useRequestReportExport(companyId);
  // Exportação passa pelo fluxo de relatórios: gera em segundo plano e avisa no sino.
  const handleExport = () => {
    requestExport.mutate({ type: "TRANSACTIONS", format: "XLSX", from: range.from, to: range.to });
  };

  return (
    <ReportShell
      title="Relatório de Distribuição de Receita"
      subtitle="Acompanhe o faturamento por tipo de procedimento, receita por profissional e preço médio."
      panelTitle="Segmentação de Receita"
      onExport={handleExport}
    >
      <div className="flex flex-wrap items-center gap-2">
        <ReportPeriodChip period={period} custom={customRange} onChange={handlePeriodChange} />
        <ReportGranularityChip value={granularity} onChange={setGranularity} />
        <ReportListChip
          icon={Stethoscope}
          label="Procedimentos"
          options={procedureOptions}
          selected={procedureIds}
          onApply={setProcedureIds}
          onClear={() => setProcedureIds([])}
        />
      </div>

      {query.isLoading ? (
        <RevenueSkeleton />
      ) : query.isError ? (
        <Card className="flex flex-col items-center gap-3 py-12 text-muted-foreground">
          <p className="text-sm">Não foi possível carregar o relatório.</p>
          <Button variant="outline" size="sm" onClick={() => query.refetch()}>
            Tentar novamente
          </Button>
        </Card>
      ) : (
        <div className="space-y-6">
          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Receita por Procedimento</h3>
            <PaidNote />
            <ReportCard
              title="Receita por Procedimento"
              legend={proceduresBuild.series.map((item) => ({
                label: truncateLabel(item.name),
                color: item.color,
              }))}
              isEmpty={proceduresBuild.data.length === 0}
            >
              <ReportLineChart
                data={proceduresBuild.data}
                xKey="period"
                series={proceduresBuild.series}
                valueFormatter={formatCompactMoney}
                labelFormatter={formatCompactMoney}
              />
            </ReportCard>
          </section>

          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Preço Médio por Procedimento</h3>
            <ReportCard
              title="Preço Médio por Procedimento"
              legend={AVERAGE_SERIES.map(({ name, color }) => ({ label: name, color }))}
              isEmpty={averageData.length === 0}
              emptyHeight={280}
            >
              <ReportBarChart
                data={averageData}
                xKey="name"
                series={AVERAGE_SERIES}
                height={300}
                tickFormatter={truncateLabel}
                valueFormatter={formatCompactMoney}
                labelFormatter={formatCompactMoney}
                xAxisAngle={-45}
              />
            </ReportCard>
          </section>

          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Receita por Profissional</h3>
            <PaidNote />
            <ReportCard
              title="Receita por Profissional"
              legend={professionalsBuild.series.map((item) => ({
                label: truncateLabel(item.name),
                color: item.color,
              }))}
              isEmpty={professionalsBuild.data.length === 0}
            >
              <ReportLineChart
                data={professionalsBuild.data}
                xKey="period"
                series={professionalsBuild.series}
                valueFormatter={formatCompactMoney}
                labelFormatter={formatCompactMoney}
              />
            </ReportCard>
          </section>
        </div>
      )}
    </ReportShell>
  );
}
