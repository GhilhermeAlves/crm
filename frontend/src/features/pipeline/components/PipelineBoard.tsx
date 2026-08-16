"use client";

import { Target } from "lucide-react";
import type {
  Opportunity,
  Stage,
  MoveDirection,
} from "../types/pipeline.types";
import { OpportunityCard } from "./OpportunityCard";
import { ScrollArea, ScrollBar } from "@/components/ui/scroll-area";

interface PipelineBoardProps {
  stages: Stage[];
  opportunities: Opportunity[];
  canMove?: boolean;
  canWin?: boolean;
  canLose?: boolean;
  canDelete?: boolean;
  busyOpportunityId?: string | null;
  onMove?: (opportunity: Opportunity, direction: MoveDirection) => void;
  onWin?: (opportunity: Opportunity) => void;
  onLost?: (opportunity: Opportunity) => void;
  onDelete?: (opportunity: Opportunity) => void;
}

export function PipelineBoard({
  stages,
  opportunities,
  canMove,
  canWin,
  canLose,
  canDelete,
  busyOpportunityId,
  onMove,
  onWin,
  onLost,
  onDelete,
}: PipelineBoardProps) {
  const grouped = new Map<string, Opportunity[]>();
  opportunities.forEach((opp) => {
    const list = grouped.get(opp.stageId) ?? [];
    list.push(opp);
    grouped.set(opp.stageId, list);
  });

  if (stages.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
        <Target className="h-12 w-12 mb-4 opacity-50" />
        <p className="text-lg font-medium">Nenhum pipeline</p>
        <p className="text-sm">Crie um pipeline para começar a vender.</p>
      </div>
    );
  }

  return (
    <ScrollArea className="w-full">
      <div className="flex gap-4 pb-4">
        {stages.map((stage, index) => {
          const columnOpps = grouped.get(stage.id) ?? [];
          const total = columnOpps.reduce((sum, o) => sum + o.value, 0);
          return (
            <div
              key={stage.id}
              className="flex w-72 shrink-0 flex-col rounded-lg border bg-muted/40"
            >
              <div
                className="flex items-center justify-between rounded-t-lg border-b px-3 py-2"
                style={{ borderTopColor: stage.color ?? undefined }}
              >
                <div className="space-y-0.5">
                  <p className="text-sm font-semibold">{stage.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {stage.probability}%
                  </p>
                </div>
                <div className="text-right text-xs text-muted-foreground">
                  <p>{columnOpps.length}</p>
                  <p className="whitespace-nowrap">
                    {new Intl.NumberFormat("pt-BR", {
                      style: "currency",
                      currency: "BRL",
                      notation: "compact",
                    }).format(total)}
                  </p>
                </div>
              </div>

              <div className="flex flex-1 flex-col gap-2 p-2">
                {columnOpps.length === 0 && (
                  <p className="py-4 text-center text-xs text-muted-foreground">
                    Sem oportunidades
                  </p>
                )}
                {columnOpps.map((opp) => (
                  <OpportunityCard
                    key={opp.id}
                    opportunity={opp}
                    isFirst={index === 0}
                    isLast={index === stages.length - 1}
                    canMove={canMove}
                    canWin={canWin}
                    canLose={canLose}
                    canDelete={canDelete}
                    busy={busyOpportunityId === opp.id}
                    onMove={(direction) => onMove?.(opp, direction)}
                    onWin={() => onWin?.(opp)}
                    onLost={() => onLost?.(opp)}
                    onDelete={() => onDelete?.(opp)}
                  />
                ))}
              </div>
            </div>
          );
        })}
      </div>
      <ScrollBar orientation="horizontal" />
    </ScrollArea>
  );
}
