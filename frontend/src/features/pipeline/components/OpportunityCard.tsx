"use client";

import {
  ChevronLeft,
  ChevronRight,
  Trophy,
  XCircle,
  Trash2,
} from "lucide-react";
import type { Opportunity } from "../types/pipeline.types";
import { formatCurrency } from "../schemas/pipeline.schema";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface OpportunityCardProps {
  opportunity: Opportunity;
  isFirst: boolean;
  isLast: boolean;
  canMove?: boolean;
  canWin?: boolean;
  canLose?: boolean;
  canDelete?: boolean;
  onMove?: (direction: "ADVANCE" | "REGRESS") => void;
  onWin?: () => void;
  onLost?: () => void;
  onDelete?: () => void;
  busy?: boolean;
}

export function OpportunityCard({
  opportunity,
  isFirst,
  isLast,
  canMove = false,
  canWin = false,
  canLose = false,
  canDelete = false,
  onMove,
  onWin,
  onLost,
  onDelete,
  busy,
}: OpportunityCardProps) {
  return (
    <Card className="shadow-sm">
      <CardHeader className="p-3 pb-1">
        <CardTitle className="text-sm font-medium leading-snug">
          {opportunity.title}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-1 p-3 pt-0">
        <p className="text-lg font-semibold">{formatCurrency(opportunity.value)}</p>
        <p className="text-xs text-muted-foreground">
          {opportunity.stageName ?? "—"} · {opportunity.probability}%
        </p>
      </CardContent>
      <CardFooter className="items-center justify-between gap-1 p-1.5 pt-0">
        <Button
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          disabled={!canMove || isFirst || busy}
          aria-label="Mover para trás"
          onClick={() => onMove?.("REGRESS")}
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>

        {(canWin || canLose) && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="h-7 text-xs" disabled={busy}>
                Concluir
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="center">
              {canWin && (
                <DropdownMenuItem onClick={onWin}>
                  <Trophy className="mr-2 h-4 w-4 text-emerald-600" />
                  Marcar como ganha
                </DropdownMenuItem>
              )}
              {canLose && (
                <DropdownMenuItem onClick={onLost}>
                  <XCircle className="mr-2 h-4 w-4 text-red-600" />
                  Marcar como perdida
                </DropdownMenuItem>
              )}
              {canDelete && (
                <DropdownMenuItem onClick={onDelete} className="text-destructive">
                  <Trash2 className="mr-2 h-4 w-4" />
                  Excluir
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        )}

        <Button
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          disabled={!canMove || isLast || busy}
          aria-label="Avançar"
          onClick={() => onMove?.("ADVANCE")}
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </CardFooter>
    </Card>
  );
}
