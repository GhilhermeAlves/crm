"use client";

import { useMemo } from "react";
import {
  format,
  startOfMonth,
  endOfMonth,
  startOfWeek,
  endOfWeek,
  addDays,
  addMonths,
  subMonths,
  isSameMonth,
  isSameDay,
  isToday,
} from "date-fns";
import { ptBR } from "date-fns/locale";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface Props {
  selected: Date;
  onSelect: (date: Date) => void;
  month: Date;
  onMonthChange: (date: Date) => void;
}

export function MiniCalendar({ selected, onSelect, month, onMonthChange }: Props) {
  const weeks = useMemo(() => {
    const start = startOfWeek(startOfMonth(month), { weekStartsOn: 1 });
    const end = endOfWeek(endOfMonth(month), { weekStartsOn: 1 });
    const days: Date[] = [];
    let day = start;
    while (day <= end) {
      days.push(day);
      day = addDays(day, 1);
    }
    const result: Date[][] = [];
    for (let i = 0; i < days.length; i += 7) {
      result.push(days.slice(i, i + 7));
    }
    return result;
  }, [month]);

  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between px-1">
        <Button
          variant="ghost"
          size="icon"
          className="h-6 w-6"
          onClick={() => onMonthChange(subMonths(month, 1))}
        >
          <ChevronLeft className="h-3.5 w-3.5" />
        </Button>
        <span className="text-xs font-medium capitalize">
          {format(month, "MMMM yyyy", { locale: ptBR })}
        </span>
        <Button
          variant="ghost"
          size="icon"
          className="h-6 w-6"
          onClick={() => onMonthChange(addMonths(month, 1))}
        >
          <ChevronRight className="h-3.5 w-3.5" />
        </Button>
      </div>

      <div className="grid grid-cols-7 text-center text-[10px] font-medium text-muted-foreground">
        {["Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom"].map((d) => (
          <div key={d} className="py-0.5">
            {d}
          </div>
        ))}
      </div>

      {weeks.map((week, wi) => (
        <div key={wi} className="grid grid-cols-7 text-center">
          {week.map((day) => {
            const inMonth = isSameMonth(day, month);
            const sel = isSameDay(day, selected);
            const today = isToday(day);
            return (
              <button
                key={day.toISOString()}
                type="button"
                onClick={() => onSelect(day)}
                className={cn(
                  "mx-auto flex h-6 w-6 items-center justify-center rounded-full text-[11px] tabular-nums transition-colors",
                  !inMonth && "text-muted-foreground/40",
                  inMonth && !sel && "hover:bg-accent",
                  today && !sel && "font-bold text-primary",
                  sel && "bg-primary text-primary-foreground",
                )}
              >
                {format(day, "d")}
              </button>
            );
          })}
        </div>
      ))}
    </div>
  );
}
