"use client";

import type { AttentionOpportunity } from "../types/dashboard.types";
import { Badge } from "@/components/ui/badge";

const formatCurrency = (value: number): string =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

type Props = {
  opportunities: AttentionOpportunity[];
  isLoading?: boolean;
};

export function AttentionList({ opportunities, isLoading }: Props) {
  if (isLoading) {
    return <p className="py-6 text-center text-sm text-muted-foreground">Carregando…</p>;
  }

  if (opportunities.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Nenhuma oportunidade precisa de atenção agora.
      </p>
    );
  }

  return (
    <ul className="divide-y divide-border">
      {opportunities.map((opp) => (
        <li key={opp.id} className="flex items-start justify-between gap-3 py-3">
          <div className="min-w-0 space-y-1">
            <p className="truncate text-sm font-medium">{opp.title}</p>
            <p className="text-xs text-muted-foreground">
              {opp.contactName ?? "—"} · {opp.stageName} · {opp.pipelineName}
            </p>
            <p className="text-xs text-amber-600 dark:text-amber-400">{opp.suggestion}</p>
          </div>
          <div className="flex shrink-0 flex-col items-end gap-1">
            <span className="text-sm font-semibold">{formatCurrency(opp.value)}</span>
            {opp.stale && <Badge variant="destructive">{opp.daysInactive} dias parada</Badge>}
          </div>
        </li>
      ))}
    </ul>
  );
}
