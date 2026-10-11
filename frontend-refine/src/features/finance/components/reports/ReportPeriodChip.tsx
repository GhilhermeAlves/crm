"use client";

import { useState } from "react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { CalendarRange } from "lucide-react";
import type { DateRange } from "react-day-picker";

import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { cn } from "@/lib/utils";

import { ReportFilterChip } from "./ReportFilterChip";
import {
  REPORT_PERIOD_OPTIONS,
  reportPeriodLabel,
  type ReportPeriodKey,
  type ReportPeriodRange,
} from "./report-period";

type ReportPeriodChipProps = {
  period: ReportPeriodKey;
  custom: ReportPeriodRange | null;
  onChange: (period: ReportPeriodKey, custom?: ReportPeriodRange) => void;
};

const parseISOSafe = (iso: string): Date | undefined => {
  const parsed = new Date(`${iso}T00:00:00`);
  return Number.isNaN(parsed.getTime()) ? undefined : parsed;
};

export function ReportPeriodChip({ period, custom, onChange }: ReportPeriodChipProps) {
  const [open, setOpen] = useState(false);
  const [showCalendar, setShowCalendar] = useState(false);
  const [draft, setDraft] = useState<DateRange | undefined>(() =>
    custom ? { from: parseISOSafe(custom.from), to: parseISOSafe(custom.to) } : undefined,
  );

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) setShowCalendar(false);
  };

  const handleShortcut = (value: ReportPeriodKey) => {
    if (value === "custom") {
      setShowCalendar(true);
      return;
    }
    onChange(value);
    setOpen(false);
  };

  const handleApply = () => {
    if (!draft?.from) return;
    onChange("custom", {
      from: format(draft.from, "yyyy-MM-dd"),
      to: format(draft.to ?? draft.from, "yyyy-MM-dd"),
    });
    setOpen(false);
  };

  return (
    <ReportFilterChip
      icon={CalendarRange}
      label="Período"
      value={reportPeriodLabel(period, custom)}
      open={open}
      onOpenChange={handleOpenChange}
      contentClassName={showCalendar ? "w-auto p-3" : "w-64"}
      align="start"
    >
      {showCalendar ? (
        <div className="space-y-3">
          <Calendar
            mode="range"
            numberOfMonths={1}
            locale={ptBR}
            defaultMonth={draft?.from}
            selected={draft}
            onSelect={setDraft}
          />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" size="sm" onClick={() => setShowCalendar(false)}>
              Voltar
            </Button>
            <Button size="sm" onClick={handleApply} disabled={!draft?.from}>
              Aplicar
            </Button>
          </div>
        </div>
      ) : (
        <ul className="p-1">
          {REPORT_PERIOD_OPTIONS.map((option) => (
            <li key={option.value}>
              <button
                type="button"
                onClick={() => handleShortcut(option.value)}
                className={cn(
                  "w-full rounded-sm px-2 py-1.5 text-left text-xs hover:bg-accent",
                  period === option.value && "font-medium text-primary",
                )}
              >
                {option.label}
              </button>
            </li>
          ))}
          <li>
            <button
              type="button"
              onClick={() => handleShortcut("custom")}
              className={cn(
                "w-full rounded-sm px-2 py-1.5 text-left text-xs hover:bg-accent",
                period === "custom" && "font-medium text-primary",
              )}
            >
              Personalizado
            </button>
          </li>
        </ul>
      )}
    </ReportFilterChip>
  );
}
