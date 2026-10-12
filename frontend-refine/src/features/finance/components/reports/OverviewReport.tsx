"use client";

import { useState } from "react";
import { User } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMembers } from "@/features/identity/members/hooks/useMembers";

import { useFinanceOverview } from "../../hooks/useFinance";
import type { ReportGranularity } from "../../types/finance.types";
import { centsToBRL } from "../../utils/money";
import { ReportBarChart, type ReportBarSeries } from "./ReportBarChart";
import { ReportCard } from "./ReportCard";
import { ReportGranularityChip } from "./ReportGranularityChip";
import { ReportListChip } from "./ReportListChip";
import { ReportNumberCard } from "./ReportNumberCard";
import { ReportPeriodChip } from "./ReportPeriodChip";
import { ReportShell } from "./ReportShell";
import { useRequestReportExport } from "@/features/reports/export/useReportExports";
import {
  formatCompactBRL,
  formatPeriodTick,
  formatReportNumber,
  formatReportPercent,
} from "./report-format";
import {
  reportPeriodLabel,
  resolveReportPeriod,
  type ReportPeriodKey,
  type ReportPeriodRange,
} from "./report-period";

const COLORS = {
  green: "#16a34a",
  red: "#dc2626",
  yellow: "#f59e0b",
  blue: "#2563eb",
};

const ATTENDANCE_SERIES: ReportBarSeries[] = [
  { key: "notAttended", name: "Não compareceu", color: COLORS.red, stackId: "attendance" },
  { key: "attended", name: "Compareceu", color: COLORS.green, stackId: "attendance" },
  { key: "pending", name: "Comparecimento pendente", color: COLORS.yellow, stackId: "attendance" },
];

const PROCEDURE_SERIES: ReportBarSeries[] = [
  { key: "done", name: "Procedimentos Concluídos", color: COLORS.green, stackId: "procedures" },
  { key: "pending", name: "Pendentes", color: COLORS.yellow, stackId: "procedures" },
];

const REVENUE_SERIES: ReportBarSeries[] = [{ key: "amount", name: "Receita", color: COLORS.blue }];

function OverviewSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-40 w-full" />
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {Array.from({ length: 5 }).map((_, index) => (
          <Skeleton key={index} className="h-24" />
        ))}
      </div>
      <div className="grid gap-4 lg:grid-cols-3">
        {Array.from({ length: 3 }).map((_, index) => (
          <Skeleton key={index} className="h-72" />
        ))}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        {Array.from({ length: 2 }).map((_, index) => (
          <Skeleton key={index} className="h-72" />
        ))}
      </div>
    </div>
  );
}

type OverviewReportProps = {
  companyId: string | null;
};

export function OverviewReport({ companyId }: OverviewReportProps) {
  const [period, setPeriod] = useState<ReportPeriodKey>("this-month");
  const [customRange, setCustomRange] = useState<ReportPeriodRange | null>(null);
  const [granularity, setGranularity] = useState<ReportGranularity>("MONTH");
  const [professionalIds, setProfessionalIds] = useState<string[]>([]);

  const range = resolveReportPeriod(period, customRange);
  const periodLabel = reportPeriodLabel(period, customRange);

  const overview = useFinanceOverview(companyId, {
    from: range.from,
    to: range.to,
    granularity,
    professionalUserIds: professionalIds,
  });
  const members = useMembers(companyId);

  const data = overview.data;
  const professionals = data?.professionals ?? [];
  const professionalOptions = (members.data ?? []).map((member) => ({
    value: member.userId,
    label: member.name,
  }));

  const attendanceData = (data?.appointmentsByAttendance ?? []).map((item) => ({
    period: formatPeriodTick(item.period, granularity),
    attended: item.attended,
    notAttended: item.notAttended,
    pending: item.pending,
  }));
  const confirmedData = (data?.confirmed ?? []).map((item) => ({
    period: formatPeriodTick(item.period, granularity),
    rate: item.rate,
  }));
  const attendedData = (data?.attended ?? []).map((item) => ({
    period: formatPeriodTick(item.period, granularity),
    rate: item.rate,
  }));
  const proceduresData = (data?.procedures ?? []).map((item) => ({
    period: formatPeriodTick(item.period, granularity),
    done: item.done,
    pending: item.pending,
  }));
  const revenueData = (data?.revenue ?? []).map((item) => ({
    period: formatPeriodTick(item.period, granularity),
    amount: item.amountCents / 100,
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
      title="Relatório geral da clínica"
      subtitle="Visualize consultas realizadas, orçamentos aprovados, procedimentos por profissional e receita gerada."
      panelTitle="Relatório Geral da Clínica"
      onExport={handleExport}
    >
      <div className="flex flex-wrap items-center gap-2">
        <ReportPeriodChip period={period} custom={customRange} onChange={handlePeriodChange} />
        <ReportGranularityChip value={granularity} onChange={setGranularity} />
        <ReportListChip
          icon={User}
          label="Profissional"
          options={professionalOptions}
          selected={professionalIds}
          onApply={setProfessionalIds}
          onClear={() => setProfessionalIds([])}
        />
      </div>

      {overview.isLoading ? (
        <OverviewSkeleton />
      ) : overview.isError ? (
        <Card className="flex flex-col items-center gap-3 py-12 text-muted-foreground">
          <p className="text-sm">Não foi possível carregar o relatório.</p>
          <Button variant="outline" size="sm" onClick={() => overview.refetch()}>
            Tentar novamente
          </Button>
        </Card>
      ) : (
        <div className="space-y-6">
          <Card>
            <div className="border-b px-4 py-3">
              <h3 className="text-sm font-semibold">Profissionais da Clínica</h3>
            </div>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-xs text-muted-foreground">
                  <th className="px-4 py-2 font-medium">Nome do Profissional</th>
                </tr>
              </thead>
              <tbody>
                {professionals.length === 0 ? (
                  <tr>
                    <td className="px-4 py-6 text-center text-xs text-muted-foreground">
                      Sem resultados
                    </td>
                  </tr>
                ) : (
                  professionals.map((professional) => (
                    <tr key={professional.id} className="border-b last:border-0">
                      <td className="px-4 py-2">{professional.name}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
            <div className="px-4 py-2 text-right text-xs text-muted-foreground">
              {professionals.length} {professionals.length === 1 ? "linha" : "linhas"}
            </div>
          </Card>

          <div className="rounded-md bg-muted px-4 py-3 text-xs text-muted-foreground">
            Estão disponíveis somente os dados gerados a partir da data de criação do profissional
            no CRM.
          </div>

          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Resumo do Período: {periodLabel}</h3>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
              <ReportNumberCard
                label="Consultas"
                value={formatReportNumber(data?.totals.appointments ?? 0)}
              />
              <ReportNumberCard
                label="Taxa de Confirmação (%)"
                value={formatReportPercent(data?.totals.confirmationRate ?? 0)}
              />
              <ReportNumberCard
                label="Taxa de Comparecimento (%)"
                value={formatReportPercent(data?.totals.attendanceRate ?? 0)}
              />
              <ReportNumberCard
                label="Procedimentos"
                value={formatReportNumber(data?.totals.proceduresDone ?? 0)}
              />
              <ReportNumberCard
                label="Receita (R$)"
                value={centsToBRL(data?.totals.revenueCents ?? 0)}
              />
            </div>
          </section>

          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Consultas</h3>
            <div className="grid gap-4 lg:grid-cols-3">
              <ReportCard
                title="Consultas por Status de Comparecimento"
                legend={ATTENDANCE_SERIES.map(({ name, color }) => ({ label: name, color }))}
                isEmpty={attendanceData.length === 0}
              >
                <ReportBarChart
                  data={attendanceData}
                  xKey="period"
                  series={ATTENDANCE_SERIES}
                  valueFormatter={formatReportNumber}
                />
              </ReportCard>
              <ReportCard
                title="Consultas Confirmadas"
                legend={[{ label: "Taxa de confirmação (%)", color: COLORS.blue }]}
                isEmpty={confirmedData.length === 0}
              >
                <ReportBarChart
                  data={confirmedData}
                  xKey="period"
                  series={[{ key: "rate", name: "Taxa de confirmação (%)", color: COLORS.blue }]}
                  valueFormatter={formatReportPercent}
                  allowDecimals
                />
              </ReportCard>
              <ReportCard
                title="Consultas Comparecidas"
                legend={[{ label: "Taxa de comparecimento (%)", color: COLORS.green }]}
                isEmpty={attendedData.length === 0}
              >
                <ReportBarChart
                  data={attendedData}
                  xKey="period"
                  series={[
                    { key: "rate", name: "Taxa de comparecimento (%)", color: COLORS.green },
                  ]}
                  valueFormatter={formatReportPercent}
                  allowDecimals
                />
              </ReportCard>
            </div>
          </section>

          <div className="grid gap-4 lg:grid-cols-2">
            <ReportCard
              title="Procedimentos"
              legend={PROCEDURE_SERIES.map(({ name, color }) => ({ label: name, color }))}
              isEmpty={proceduresData.length === 0}
            >
              <ReportBarChart
                data={proceduresData}
                xKey="period"
                series={PROCEDURE_SERIES}
                valueFormatter={formatReportNumber}
              />
            </ReportCard>
            <ReportCard
              title="Receita"
              legend={REVENUE_SERIES.map(({ name, color }) => ({ label: name, color }))}
              isEmpty={revenueData.length === 0}
            >
              <ReportBarChart
                data={revenueData}
                xKey="period"
                series={REVENUE_SERIES}
                valueFormatter={formatCompactBRL}
              />
            </ReportCard>
          </div>
        </div>
      )}
    </ReportShell>
  );
}
