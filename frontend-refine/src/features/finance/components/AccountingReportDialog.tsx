"use client";

import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

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

import { useAvailableMonths, useGenerateAccountingReport } from "../hooks/useFinance";
import type { AccountingReportType, ReportFormat } from "../types/finance.types";
import { downloadBlob, formatMonthLabel, previousMonthKey } from "../utils/accounting-report";

type AccountingReportDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string | null;
};

type ReportTypeOption = {
  value: AccountingReportType;
  title: string;
  description: string;
};

const REPORT_TYPE_OPTIONS: ReportTypeOption[] = [
  {
    value: "MONTHLY_ACCOUNTING",
    title: "Relatório Contábil Mensal",
    description: "Receitas e despesas do mês por categoria, para DRE ou Carnê-Leão.",
  },
  {
    value: "STATEMENT",
    title: "Extrato Financeiro",
    description: "Mostra o que entrou e saiu do caixa da clínica no mês.",
  },
  {
    value: "TRANSACTIONS",
    title: "Detalhamento por Transação",
    description: "Cada lançamento em tabela, com datas, valores, taxas e parcelas.",
  },
];

const FORMAT_OPTIONS: { value: ReportFormat; label: string }[] = [
  { value: "PDF", label: "PDF" },
  { value: "CSV", label: "CSV" },
];

export function AccountingReportDialog({
  open,
  onOpenChange,
  companyId,
}: AccountingReportDialogProps) {
  const [type, setType] = useState<AccountingReportType | "">("");
  const [month, setMonth] = useState("");
  const [format, setFormat] = useState<ReportFormat | "">("");

  const monthsQuery = useAvailableMonths(companyId);
  const months = monthsQuery.data ?? [];
  const generate = useGenerateAccountingReport(companyId);

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
    generate.mutate(
      { type, month, format },
      {
        onSuccess: ({ blob, fileName }) => {
          downloadBlob(blob, fileName);
          onOpenChange(false);
          toast.success("Relatório gerado");
        },
        onError: (error: Error) => {
          toast.error(error.message || "Erro ao gerar o relatório.");
        },
      },
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85dvh] overflow-y-auto sm:max-w-[420px]">
        <DialogHeader>
          <DialogTitle>Gerar relatórios contábeis</DialogTitle>
          <DialogDescription>
            Baixe o resultado mensal da sua clínica em um dos três formatos abaixo.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5">
          <section className="space-y-2">
            <h3 className="text-sm font-semibold">Tipo de relatório</h3>
            <RadioGroup
              value={type}
              onValueChange={(value) => setType(value as AccountingReportType)}
            >
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
              onValueChange={(value) => setFormat(value as ReportFormat)}
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
              "Gerar e baixar"
            )}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
