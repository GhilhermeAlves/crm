"use client";

import type { LeadStatus } from "../types/lead.types";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const statusConfig: Record<LeadStatus, { label: string; className: string }> = {
  NEW: {
    label: "Novo",
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
  CONTACTED: {
    label: "Contatado",
    className: "bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400",
  },
  QUALIFIED: {
    label: "Qualificado",
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  UNQUALIFIED: {
    label: "Não qualificado",
    className: "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400",
  },
  CONVERTED: {
    label: "Convertido",
    className: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  },
  LOST: {
    label: "Perdido",
    className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  },
};

export function LeadStatusBadge({ status }: { status: LeadStatus }) {
  const config = statusConfig[status] || statusConfig.NEW;
  return (
    <Badge variant="outline" className={cn("font-medium", config.className)}>
      {config.label}
    </Badge>
  );
}

export function LeadSourceBadge({ source }: { source: string }) {
  return (
    <Badge variant="secondary" className="font-medium">
      {source}
    </Badge>
  );
}

export function LeadClassificationBadge({ classification }: { classification: string | null }) {
  if (!classification) {
    return <span className="text-sm text-muted-foreground">—</span>;
  }
  const label =
    {
      HOT: "Quente",
      WARM: "Morno",
      COLD: "Frio",
      DISQUALIFIED: "Desqualificado",
    }[classification] ?? classification;
  return <Badge variant="outline">{label}</Badge>;
}
