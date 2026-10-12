"use client";

import { useState } from "react";
import { format, subMonths } from "date-fns";
import { FileSpreadsheet, Loader2, Printer } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { resolveReportPeriod } from "@/features/finance/components/reports/report-period";

import type { ExportFormat, ReportExportDefinition, ReportExportRequest } from "./types";

type ReportExportFormProps = {
  definition: ReportExportDefinition;
  isSubmitting: boolean;
  onSubmit: (request: ReportExportRequest) => void;
};

const SHORTCUTS = [
  { key: "this-month", label: "Este mês" },
  { key: "last-month", label: "Mês passado" },
  { key: "last-3-months", label: "Últimos 3 meses" },
] as const;

/** Escolha do período e dos botões Imprimir (PDF) / Exportar (Excel). */
export function ReportExportForm({ definition, isSubmitting, onSubmit }: ReportExportFormProps) {
  const initialRange = resolveReportPeriod("last-month");
  const [month, setMonth] = useState(() => format(subMonths(new Date(), 1), "yyyy-MM"));
  const [from, setFrom] = useState(initialRange.from);
  const [to, setTo] = useState(initialRange.to);
  const [pendingFormat, setPendingFormat] = useState<ExportFormat | null>(null);

  const isMonth = definition.period === "month";
  const rangeInvalid = !isMonth && (!from || !to || to < from);
  const disabled = isSubmitting || (isMonth ? !month : rangeInvalid);

  const submit = (exportFormat: ExportFormat) => {
    setPendingFormat(exportFormat);
    onSubmit(
      isMonth
        ? { type: definition.type, format: exportFormat, month }
        : { type: definition.type, format: exportFormat, from, to },
    );
  };

  return (
    <Card className="space-y-5 p-5">
      <section className="space-y-3">
        <h2 className="text-sm font-semibold">Período</h2>
        {isMonth ? (
          <div className="max-w-xs space-y-1.5">
            <Label htmlFor="report-month">Mês de referência</Label>
            <Input
              id="report-month"
              type="month"
              value={month}
              onChange={(event) => setMonth(event.target.value)}
            />
          </div>
        ) : (
          <>
            <div className="flex flex-wrap gap-2">
              {SHORTCUTS.map((shortcut) => (
                <Button
                  key={shortcut.key}
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    const range = resolveReportPeriod(shortcut.key);
                    setFrom(range.from);
                    setTo(range.to);
                  }}
                >
                  {shortcut.label}
                </Button>
              ))}
            </div>
            <div className="grid max-w-md grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="report-from">De</Label>
                <Input
                  id="report-from"
                  type="date"
                  value={from}
                  onChange={(event) => setFrom(event.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="report-to">Até</Label>
                <Input
                  id="report-to"
                  type="date"
                  value={to}
                  onChange={(event) => setTo(event.target.value)}
                />
              </div>
            </div>
            {rangeInvalid && (
              <p className="text-xs text-destructive">
                A data final deve ser igual ou posterior à inicial.
              </p>
            )}
          </>
        )}
      </section>

      <div className="flex flex-col gap-2 sm:flex-row">
        <Button type="button" disabled={disabled} onClick={() => submit("PDF")}>
          {isSubmitting && pendingFormat === "PDF" ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Printer className="h-4 w-4" />
          )}
          Imprimir (PDF)
        </Button>
        <Button type="button" variant="outline" disabled={disabled} onClick={() => submit("XLSX")}>
          {isSubmitting && pendingFormat === "XLSX" ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <FileSpreadsheet className="h-4 w-4" />
          )}
          Exportar (Excel)
        </Button>
      </div>
      <p className="text-xs text-muted-foreground">
        O arquivo é gerado em segundo plano. Quando ficar pronto, o link de download aparece no sino
        de notificações.
      </p>
    </Card>
  );
}
