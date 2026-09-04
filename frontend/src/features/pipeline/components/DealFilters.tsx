"use client";

import { FilterX } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { dealStages, forecastCategories } from "../schemas/deal.schema";

type DealFiltersProps = {
  stage: string;
  responsible: string;
  forecastCategory: string;
  responsibles: string[];
  onStageChange: (value: string) => void;
  onResponsibleChange: (value: string) => void;
  onForecastChange: (value: string) => void;
  onClear: () => void;
};

export function DealFilters({
  stage,
  responsible,
  forecastCategory,
  responsibles,
  onStageChange,
  onResponsibleChange,
  onForecastChange,
  onClear,
}: DealFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Select value={stage} onValueChange={onStageChange}>
        <SelectTrigger className="w-full sm:w-40">
          <SelectValue placeholder="Etapa" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">Todas as etapas</SelectItem>
          {dealStages.map((s) => (
            <SelectItem key={s} value={s}>
              {s}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select value={responsible} onValueChange={onResponsibleChange}>
        <SelectTrigger className="w-full sm:w-40">
          <SelectValue placeholder="Responsável" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">Todos</SelectItem>
          {responsibles.map((r) => (
            <SelectItem key={r} value={r}>
              {r}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select value={forecastCategory} onValueChange={onForecastChange}>
        <SelectTrigger className="w-full sm:w-44">
          <SelectValue placeholder="Categoria" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">Todas</SelectItem>
          {forecastCategories.map((c) => (
            <SelectItem key={c} value={c}>
              {c}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Button variant="ghost" size="sm" onClick={onClear}>
        <FilterX className="mr-1 h-4 w-4" />
        Limpar
      </Button>
    </div>
  );
}
