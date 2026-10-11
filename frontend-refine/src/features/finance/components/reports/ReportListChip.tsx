"use client";

import { useState } from "react";
import { Search, type LucideIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";

import { ReportFilterChip } from "./ReportFilterChip";

export type ReportFilterOption = {
  value: string;
  label: string;
};

type ReportListChipProps = {
  icon: LucideIcon;
  label: string;
  options: ReportFilterOption[];
  selected: string[];
  onApply: (selected: string[]) => void;
  onClear: () => void;
  searchPlaceholder?: string;
};

export function ReportListChip({
  icon,
  label,
  options,
  selected,
  onApply,
  onClear,
  searchPlaceholder = "Pesquisar na lista",
}: ReportListChipProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [draft, setDraft] = useState<string[]>(selected);

  const handleOpenChange = (next: boolean) => {
    if (next) {
      setDraft(selected);
      setQuery("");
    }
    setOpen(next);
  };

  const normalizedQuery = query.trim().toLowerCase();
  const filtered = options.filter((option) => option.label.toLowerCase().includes(normalizedQuery));
  const allSelected = filtered.length > 0 && filtered.every((o) => draft.includes(o.value));

  const toggle = (value: string) =>
    setDraft((prev) =>
      prev.includes(value) ? prev.filter((item) => item !== value) : [...prev, value],
    );

  const toggleAll = () => {
    if (allSelected) {
      setDraft((prev) => prev.filter((value) => !filtered.some((o) => o.value === value)));
      return;
    }
    setDraft((prev) => Array.from(new Set([...prev, ...filtered.map((o) => o.value)])));
  };

  const displayValue =
    selected.length === 0
      ? null
      : selected.length === 1
        ? (options.find((o) => o.value === selected[0])?.label ?? "1 selecionado")
        : `${selected.length} selecionados`;

  return (
    <ReportFilterChip
      icon={icon}
      label={label}
      value={displayValue}
      onClear={onClear}
      open={open}
      onOpenChange={handleOpenChange}
      contentClassName="w-64"
      align="start"
    >
      <div className="space-y-2 p-2">
        <div className="relative">
          <Search className="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={searchPlaceholder}
            aria-label={searchPlaceholder}
            className="h-8 pl-7 text-xs"
          />
        </div>

        <label className="flex cursor-pointer items-center gap-2 py-1 text-xs font-medium">
          <Checkbox checked={allSelected} onCheckedChange={toggleAll} className="h-3.5 w-3.5" />
          Selecionar tudo
        </label>

        <div className="max-h-48 space-y-0.5 overflow-y-auto">
          {filtered.length === 0 ? (
            <p className="px-1 py-2 text-xs text-muted-foreground">Nenhum item encontrado.</p>
          ) : (
            filtered.map((option) => (
              <label
                key={option.value}
                className="flex cursor-pointer items-center gap-2 rounded px-1 py-1 text-xs hover:bg-muted"
              >
                <Checkbox
                  checked={draft.includes(option.value)}
                  onCheckedChange={() => toggle(option.value)}
                  className="h-3.5 w-3.5"
                />
                <span className="truncate">{option.label}</span>
              </label>
            ))
          )}
        </div>

        <Button
          size="sm"
          className="w-full"
          onClick={() => {
            onApply(draft);
            setOpen(false);
          }}
        >
          Adicionar filtro
        </Button>
      </div>
    </ReportFilterChip>
  );
}
