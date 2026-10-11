"use client";

import { useState } from "react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { CalendarIcon, ChevronDown } from "lucide-react";
import type { DateRange } from "react-day-picker";

import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";

import {
  PERIOD_SHORTCUTS,
  periodLabel,
  type DateRangeValue,
  type PeriodKey,
} from "../utils/period";

type PeriodPickerProps = {
  period: PeriodKey;
  custom: DateRangeValue | null;
  onChange: (period: PeriodKey, custom?: DateRangeValue) => void;
};

const parseISOSafe = (iso: string): Date | undefined => {
  const parsed = new Date(`${iso}T00:00:00`);
  return Number.isNaN(parsed.getTime()) ? undefined : parsed;
};

export function PeriodPicker({ period, custom, onChange }: PeriodPickerProps) {
  const [open, setOpen] = useState(false);
  const [showCalendar, setShowCalendar] = useState(false);
  const [draft, setDraft] = useState<DateRange | undefined>(() =>
    custom ? { from: parseISOSafe(custom.from), to: parseISOSafe(custom.to) } : undefined,
  );

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) setShowCalendar(false);
  };

  const handleShortcut = (value: PeriodKey) => {
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
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <Button variant="outline" className="gap-2 font-normal">
          <CalendarIcon className="h-4 w-4 text-muted-foreground" />
          {periodLabel(period, custom)}
          <ChevronDown className="h-4 w-4 text-muted-foreground" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className={cn(showCalendar ? "w-auto p-3" : "w-56 p-1")}>
        {showCalendar ? (
          <div className="space-y-3">
            <Calendar
              mode="range"
              numberOfMonths={2}
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
          <ul className="text-sm">
            {PERIOD_SHORTCUTS.map((shortcut) => (
              <li key={shortcut.value}>
                <button
                  type="button"
                  onClick={() => handleShortcut(shortcut.value)}
                  className={cn(
                    "w-full rounded-sm px-2 py-1.5 text-left transition-colors hover:bg-accent",
                    period === shortcut.value && "font-medium text-primary",
                  )}
                >
                  {shortcut.label}
                </button>
              </li>
            ))}
          </ul>
        )}
      </PopoverContent>
    </Popover>
  );
}
