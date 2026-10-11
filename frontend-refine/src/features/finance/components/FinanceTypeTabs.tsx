"use client";

import { cn } from "@/lib/utils";

export type FinanceTab = "all" | "income" | "expense";

const TABS: { value: FinanceTab; label: string }[] = [
  { value: "all", label: "Tudo" },
  { value: "income", label: "Receitas" },
  { value: "expense", label: "Despesas" },
];

type FinanceTypeTabsProps = {
  value: FinanceTab;
  onChange: (value: FinanceTab) => void;
};

export function FinanceTypeTabs({ value, onChange }: FinanceTypeTabsProps) {
  return (
    <div
      role="tablist"
      aria-label="Tipo de lançamento"
      className="inline-flex items-center gap-1 rounded-full border bg-card p-1"
    >
      {TABS.map((tab) => {
        const active = value === tab.value;
        return (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(tab.value)}
            className={cn(
              "rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors",
              active
                ? "bg-primary text-primary-foreground shadow-sm"
                : "text-muted-foreground hover:bg-muted hover:text-foreground",
            )}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
