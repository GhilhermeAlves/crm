"use client";

import { useState } from "react";
import { CalendarRange } from "lucide-react";

import { cn } from "@/lib/utils";

import type { ReportGranularity } from "../../types/finance.types";
import { ReportFilterChip } from "./ReportFilterChip";

export const GRANULARITY_OPTIONS: { value: ReportGranularity; label: string }[] = [
  { value: "DAY", label: "Dia" },
  { value: "WEEK", label: "Semana" },
  { value: "MONTH", label: "Mês" },
];

type ReportGranularityChipProps = {
  value: ReportGranularity;
  onChange: (value: ReportGranularity) => void;
};

export function ReportGranularityChip({ value, onChange }: ReportGranularityChipProps) {
  const [open, setOpen] = useState(false);
  const current = GRANULARITY_OPTIONS.find((option) => option.value === value);

  return (
    <ReportFilterChip
      icon={CalendarRange}
      label="Dia/Semana/Mês"
      value={current?.label ?? "Mês"}
      open={open}
      onOpenChange={setOpen}
      contentClassName="w-40"
      align="start"
    >
      <ul className="p-1">
        {GRANULARITY_OPTIONS.map((option) => (
          <li key={option.value}>
            <button
              type="button"
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
              className={cn(
                "w-full rounded-sm px-2 py-1.5 text-left text-xs hover:bg-accent",
                value === option.value && "font-medium text-primary",
              )}
            >
              {option.label}
            </button>
          </li>
        ))}
      </ul>
    </ReportFilterChip>
  );
}
