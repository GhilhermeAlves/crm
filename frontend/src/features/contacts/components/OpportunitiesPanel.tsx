"use client";

import type { OpportunityItem } from "../types/contact.types";
import { OPPORTUNITY_STATUS_LABELS } from "../types/contact.types";
import { Badge } from "@/components/ui/badge";

const formatCurrency = (value: number): string =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(
    value ?? 0,
  );

const STATUS_VARIANT: Record<
  string,
  "default" | "secondary" | "destructive" | "outline"
> = {
  OPEN: "default",
  WON: "secondary",
  LOST: "destructive",
};

export function OpportunitiesPanel({
  opportunities,
  openCount,
  openValue,
}: {
  opportunities: OpportunityItem[];
  openCount: number;
  openValue: number;
}) {
  if (opportunities.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Nenhuma oportunidade associada a este contato.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between rounded-lg bg-muted px-3 py-2 text-sm">
        <span className="font-medium">{openCount} em aberto</span>
        <span className="font-semibold">
          {formatCurrency(openValue)} em jogo
        </span>
      </div>
      <ul className="divide-y divide-border">
        {opportunities.map((opp) => (
          <li
            key={opp.id}
            className="flex items-start justify-between gap-3 py-3"
          >
            <div className="min-w-0 space-y-1">
              <p className="truncate text-sm font-medium">{opp.title}</p>
              <p className="text-xs text-muted-foreground">
                {opp.pipelineName} · {opp.stageName} · {opp.probability}%
              </p>
              {opp.expectedCloseDate && (
                <p className="text-xs text-muted-foreground">
                  Fechamento:{" "}
                  {new Intl.DateTimeFormat("pt-BR", {
                    dateStyle: "medium",
                  }).format(new Date(opp.expectedCloseDate))}
                </p>
              )}
            </div>
            <div className="flex shrink-0 flex-col items-end gap-1">
              <span className="text-sm font-semibold">
                {formatCurrency(opp.value)}
              </span>
              <Badge variant={STATUS_VARIANT[opp.status] ?? "outline"}>
                {opp.statusLabel ?? OPPORTUNITY_STATUS_LABELS[opp.status]}
              </Badge>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
