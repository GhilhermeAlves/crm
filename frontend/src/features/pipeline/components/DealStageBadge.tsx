"use client";

import type { DealStage } from "../types/deal.types";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const stageConfig: Record<DealStage, { label: string; className: string }> = {
  Novo: {
    label: "Novo",
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
  Descoberta: {
    label: "Descoberta",
    className: "bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400",
  },
  Proposta: {
    label: "Proposta",
    className: "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400",
  },
  Negociação: {
    label: "Negociação",
    className: "bg-violet-100 text-violet-800 dark:bg-violet-900/30 dark:text-violet-400",
  },
  "Fechado/Ganho": {
    label: "Fechado/Ganho",
    className: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  },
  Perdido: {
    label: "Perdido",
    className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  },
};

export function DealStageBadge({ stage }: { stage: DealStage }) {
  const config = stageConfig[stage] ?? stageConfig.Novo;
  return (
    <Badge variant="outline" className={cn("whitespace-nowrap font-medium", config.className)}>
      {config.label}
    </Badge>
  );
}
