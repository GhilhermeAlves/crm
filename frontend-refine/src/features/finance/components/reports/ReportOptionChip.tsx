"use client";

import { useState } from "react";
import type { LucideIcon } from "lucide-react";

import { cn } from "@/lib/utils";

import { ReportFilterChip } from "./ReportFilterChip";
import type { ReportFilterOption } from "./ReportListChip";

type ReportOptionChipProps = {
  icon: LucideIcon;
  label: string;
  value: string | null;
  options: ReportFilterOption[];
  onChange: (value: string) => void;
  onClear?: () => void;
};

export function ReportOptionChip({
  icon,
  label,
  value,
  options,
  onChange,
  onClear,
}: ReportOptionChipProps) {
  const [open, setOpen] = useState(false);
  const current = options.find((option) => option.value === value);

  return (
    <ReportFilterChip
      icon={icon}
      label={label}
      value={current?.label ?? null}
      onClear={onClear}
      open={open}
      onOpenChange={setOpen}
      contentClassName="w-56"
      align="start"
    >
      <ul className="p-1">
        {options.length === 0 ? (
          <li className="px-2 py-1.5 text-xs text-muted-foreground">Nenhuma opção disponível.</li>
        ) : (
          options.map((option) => (
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
          ))
        )}
      </ul>
    </ReportFilterChip>
  );
}
