"use client";

import { useMemo, useState } from "react";
import { ListChecks, Tag, User } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { useAppointmentTypes } from "@/features/sales/scheduling/hooks/useScheduling";
import {
  APPOINTMENT_STATUS_LABELS,
  type AppointmentStatus,
} from "@/features/sales/scheduling/types/scheduling.types";

import { useAgendaReport } from "../../hooks/useFinance";
import type { ReportGranularity } from "../../types/finance.types";
import { ReportBarChart } from "./ReportBarChart";
import { ReportCard } from "./ReportCard";
import { ReportGranularityChip } from "./ReportGranularityChip";
import { ReportListChip, type ReportFilterOption } from "./ReportListChip";
import { ReportNumberCard } from "./ReportNumberCard";
import { ReportOptionChip } from "./ReportOptionChip";
import { ReportPeriodChip } from "./ReportPeriodChip";
import { ReportShell } from "./ReportShell";
import { downloadCsv, type CsvValue } from "./report-csv";
import { formatReportNumber, formatReportPercent } from "./report-format";
import {
  reportPeriodLabel,
  resolveReportPeriod,
  type ReportPeriodKey,
  type ReportPeriodRange,
} from "./report-period";

const STATUS_OPTIONS: ReportFilterOption[] = [
  { value: "CANCELADO", label: "Cancelado" },
  { value: "CONFIRMADO", label: "Confirmado" },
  { value: "PENDENTE", label: "Pendente" },
];

const STATUS_SERIES = [{ key: "count", name: "Agendamentos", color: "#2563eb" }];

function AgendaSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <Skeleton key={index} className="h-24" />
        ))}
      </div>
      <Skeleton className="h-72 w-full" />
    </div>
  );
}

type AgendaReportProps = {
  companyId: string | null;
};

export function AgendaReport({ companyId }: AgendaReportProps) {
  const [period, setPeriod] = useState<ReportPeriodKey>("this-month");
  const [customRange, setCustomRange] = useState<ReportPeriodRange | null>(null);
  const [granularity, setGranularity] = useState<ReportGranularity>("MONTH");
  const [appointmentType, setAppointmentType] = useState<string | null>(null);
  const [statuses, setStatuses] = useState<string[]>([]);
  const [professionalIds, setProfessionalIds] = useState<string[]>([]);

  const range = resolveReportPeriod(period, customRange);
  const periodLabel = reportPeriodLabel(period, customRange);

  const types = useAppointmentTypes(companyId);
  const members = useMembers(companyId);

  const typeOptions = useMemo<ReportFilterOption[]>(
    () => (types.data ?? []).map((type) => ({ value: type.id, label: type.name })),
    [types.data],
  );
  const defaultTypeId = useMemo(() => {
    const consultation = typeOptions.find((option) =>
      option.label.toLowerCase().includes("consulta"),
    );
    return consultation?.value ?? typeOptions[0]?.value ?? null;
  }, [typeOptions]);

  const effectiveType =
    appointmentType === null ? defaultTypeId : appointmentType === "" ? null : appointmentType;

  const professionalOptions = (members.data ?? []).map((member) => ({
    value: member.userId,
    label: member.name,
  }));

  const agenda = useAgendaReport(companyId, {
    from: range.from,
    to: range.to,
    granularity,
    appointmentType: effectiveType ?? undefined,
    statuses,
    professionalUserIds: professionalIds,
  });

  const data = agenda.data;
  const byStatus = data?.byStatus ?? [];
  const statusData = byStatus.map((item) => ({
    status: APPOINTMENT_STATUS_LABELS[item.status as AppointmentStatus] ?? item.status,
    count: item.count,
  }));

  const handlePeriodChange = (next: ReportPeriodKey, nextCustom?: ReportPeriodRange) => {
    setPeriod(next);
    setCustomRange(next === "custom" ? (nextCustom ?? null) : null);
  };

  const handleExport = () => {
    const rows: CsvValue[][] = [
      ["Relatório da Agenda"],
      ["Período", periodLabel],
      [],
      ["Indicador", "Valor"],
      ["Agendamentos Totais", data?.total ?? 0],
      ["Agendamentos Com Comparecimento", data?.attended ?? 0],
      ["Agendamentos Sem Comparecimento", data?.notAttended ?? 0],
      ["Taxa de Comparecimento (%)", data?.attendanceRate ?? 0],
      [],
      ["Agendamentos por Status"],
      ["Status", "Quantidade"],
      ...byStatus.map((item) => [
        APPOINTMENT_STATUS_LABELS[item.status as AppointmentStatus] ?? item.status,
        item.count,
      ]),
    ];
    downloadCsv("relatorio-agenda.csv", rows);
  };

  return (
    <ReportShell
      title="Relatório da Agenda"
      subtitle="Veja o total de agendamentos, confirmações e comparecimentos."
      panelTitle="Relatório da Agenda"
      onExport={handleExport}
    >
      <div className="flex flex-wrap items-center gap-2">
        <ReportPeriodChip period={period} custom={customRange} onChange={handlePeriodChange} />
        <ReportGranularityChip value={granularity} onChange={setGranularity} />
        <ReportOptionChip
          icon={Tag}
          label="Tipo de Agendamento"
          value={effectiveType}
          options={typeOptions}
          onChange={(value) => setAppointmentType(value)}
        />
        <ReportListChip
          icon={ListChecks}
          label="Status do Agendamento"
          options={STATUS_OPTIONS}
          selected={statuses}
          onApply={setStatuses}
          onClear={() => setStatuses([])}
        />
        <ReportListChip
          icon={User}
          label="Profissional"
          options={professionalOptions}
          selected={professionalIds}
          onApply={setProfessionalIds}
          onClear={() => setProfessionalIds([])}
        />
      </div>

      {agenda.isLoading ? (
        <AgendaSkeleton />
      ) : agenda.isError ? (
        <Card className="flex flex-col items-center gap-3 py-12 text-muted-foreground">
          <p className="text-sm">Não foi possível carregar o relatório.</p>
          <Button variant="outline" size="sm" onClick={() => agenda.refetch()}>
            Tentar novamente
          </Button>
        </Card>
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <ReportNumberCard
              label="Agendamentos Totais"
              value={formatReportNumber(data?.total ?? 0)}
            />
            <ReportNumberCard
              label="Agendamentos Com Comparecimento"
              value={formatReportNumber(data?.attended ?? 0)}
            />
            <ReportNumberCard
              label="Agendamentos Sem Comparecimento"
              value={formatReportNumber(data?.notAttended ?? 0)}
            />
            <ReportNumberCard
              label="Taxa de Comparecimento"
              value={formatReportPercent(data?.attendanceRate ?? 0)}
            />
          </div>

          <ReportCard
            title="Agendamentos por Status"
            legend={[{ label: "Agendamentos", color: "#2563eb" }]}
            isEmpty={statusData.length === 0}
            emptyHeight={240}
          >
            <ReportBarChart
              data={statusData}
              xKey="status"
              series={STATUS_SERIES}
              valueFormatter={formatReportNumber}
              height={240}
              barSize={40}
            />
          </ReportCard>
        </div>
      )}
    </ReportShell>
  );
}
