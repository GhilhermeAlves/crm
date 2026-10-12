"use client";

import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

import { useAvailableMonths } from "../hooks/useFinance";
import type { ExportFormat, ReportExportType } from "@/features/reports/export/types";
import { useRequestReportExport } from "@/features/reports/export/useReportExports";
import { formatMonthLabel, previousMonthKey } from "../utils/accounting-report";

type AccountingReportDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string | null;
};

type ReportTypeOption = {
  value: ReportExportType;
  title: string;
  description: string;
};

const REPORT_TYPE_OPTIONS: ReportTypeOption[] = [
  {
    value: "MONTHLY_DRE",
    title: "Contábil mensal (DRE)",
    description: "Receitas e despesas do mês por categoria, para DRE ou Carnê-Leão.",
  },
  {
    value: "CASH_FLOW",
    title: "Fluxo de caixa",
    description: "O que entrou e saiu do caixa da clínica, dia a dia, no mês.",
  },
  {
    value: "TRANSACTIONS",
    title: "Extrato de lançamentos",
    description: "Cada lançamento do mês em tabela, com paciente, categoria, valor e status.",
  },
];

const FORMAT_OPTIONS: { value: ExportFormat; label: string }[] = [
  { value: "PDF", label: "PDF" },
  { value: "XLSX", label: "Excel" },
];

export function AccountingReportDialog({
  open,
  onOpenChange,
  companyId,
}: AccountingReportDialogProps) {
  const [type, setType] = useState<ReportExportType | "">("");
  const [month, setMonth] = useState("");
  const [format, setFormat] = useState<ExportFormat | "">("");

  const monthsQuery = useAvailableMonths(companyId);
  const months = monthsQuery.data ?? [];
  const generate = useRequestReportExport(companyId);

  // Ao fechar, as escolhas voltam ao estado inicial.
  useEffect(() => {
    if (open) return;
    setType("");
    setMonth("");
    setFormat("");
  }, [open]);

  // Pré-seleciona o mês anterior quando ele está disponível na lista.
  useEffect(() => {
    if (!open || month) return;
    const previous = previousMonthKey();
    if (monthsQuery.data?.includes(previous)) setMonth(previous);
  }, [open, month, monthsQuery.data]);

  const hasMonths = months.length > 0;
  const isGenerating = generate.isPending;
  const canGenerate = !!type && !!month && !!format && hasMonths && !isGenerating;

  const handleGenerate = () => {
    if (!type || !month || !format) return;
    // O arquivo é gerado em segundo plano e chega pelo sino (toast no hook).
    // DRE é por mês; os demais tipos recebem o mês como intervalo de datas.
    const [year, monthNumber] = month.split("-").map(Number);
    const lastDay = new Date(year, monthNumber, 0).getDate();
    const request =
      type === "MONTHLY_DRE"
        ? { type, month, format }
        : { type, format, from: `${month}-01`, to: `${month}-${String(lastDay).padStart(2, "0")}` };
    generate.mutate(request, { onSuccess: () => onOpenChange(false) });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85dvh] overflow-y-auto sm:max-w-[420px]">
        <DialogHeader>
          <DialogTitle>Gerar relatórios contábeis</DialogTitle>
          <DialogDescription>
            Gere o resultado mensal da sua clínica. Quando o arquivo ficar pronto, o link de
            download aparece no sino de notificações.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5">
          <section className="space-y-2">
            <h3 className="text-sm font-semibold">Tipo de relatório</h3>
            <RadioGroup value={type} onValueChange={(value) => setType(value as ReportExportType)}>
              {REPORT_TYPE_OPTIONS.map((option) => (
                <label
                  key={option.value}
                  className={cn(
                    "flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors",
                    type === option.value
                      ? "border-primary bg-primary/5"
                      : "border-input hover:bg-muted/40",
                  )}
                >
                  <span className="flex-1 space-y-1">
                    <span className="block text-sm font-semibold">{option.title}</span>
                    <span className="block text-xs text-muted-foreground">
                      {option.description}
                    </span>
                  </span>
                  <RadioGroupItem value={option.value} className="mt-0.5" />
                </label>
              ))}
            </RadioGroup>
          </section>

          <section className="space-y-2">
            <h3 className="text-sm font-semibold">Período contábil</h3>
            {monthsQuery.isLoading ? (
              <Skeleton className="h-10 w-full" />
            ) : hasMonths ? (
              <Select value={month} onValueChange={setMonth}>
                <SelectTrigger aria-label="Período contábil">
                  <SelectValue placeholder="Selecione o mês" />
                </SelectTrigger>
                <SelectContent>
                  {months.map((value) => (
                    <SelectItem key={value} value={value}>
                      {formatMonthLabel(value)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            ) : (
              <p className="text-sm text-muted-foreground">
                Ainda não há lançamentos para gerar relatórios.
              </p>
            )}
          </section>

          <section className="space-y-2">
            <h3 className="text-sm font-semibold">Formato do relatório</h3>
            <RadioGroup
              value={format}
              onValueChange={(value) => setFormat(value as ExportFormat)}
              className="flex gap-6"
            >
              {FORMAT_OPTIONS.map((option) => (
                <div key={option.value} className="flex items-center gap-2">
                  <RadioGroupItem value={option.value} id={`report-format-${option.value}`} />
                  <Label
                    htmlFor={`report-format-${option.value}`}
                    className="cursor-pointer text-sm font-normal"
                  >
                    {option.label}
                  </Label>
                </div>
              ))}
            </RadioGroup>
          </section>

          <Button type="button" className="w-full" disabled={!canGenerate} onClick={handleGenerate}>
            {isGenerating ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Gerando…
              </>
            ) : (
              "Gerar relatório"
            )}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
