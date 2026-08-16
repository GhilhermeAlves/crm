"use client";

import { History } from "lucide-react";
import type {
  WorkflowExecution,
  WorkflowExecutionStatus,
} from "../types/workflow.types";
import {
  WORKFLOW_EXECUTION_STATUS_LABELS,
  WORKFLOW_ACTION_LABELS,
} from "../types/workflow.types";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";

interface WorkflowExecutionsPanelProps {
  data?: WorkflowExecution[];
  isLoading?: boolean;
}

function StatusBadge({ status }: { status: WorkflowExecutionStatus }) {
  const variant =
    status === "SUCCESS"
      ? "default"
      : status === "FAILED"
        ? "destructive"
        : "outline";
  return (
    <Badge variant={variant}>{WORKFLOW_EXECUTION_STATUS_LABELS[status]}</Badge>
  );
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function eventLabel(eventType: string): string {
  const map: Record<string, string> = {
    OPPORTUNITY_CREATED: "Oportunidade criada",
    OPPORTUNITY_STAGE_CHANGED: "Etapa alterada",
    OPPORTUNITY_WON: "Oportunidade ganha",
    OPPORTUNITY_LOST: "Oportunidade perdida",
    TASK_CREATED: "Tarefa criada",
    TASK_COMPLETED: "Tarefa concluída",
    ACTIVITY_CREATED: "Atividade criada",
  };
  return map[eventType] ?? eventType;
}

export function WorkflowExecutionsPanel({
  data = [],
  isLoading,
}: WorkflowExecutionsPanelProps) {
  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-12 animate-pulse rounded bg-muted" />
        ))}
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
        <History className="mb-4 h-12 w-12 opacity-50" />
        <p className="text-lg font-medium">Nenhuma execução registrada</p>
        <p className="text-sm">
          As execuções aparecerão aqui quando o workflow disparar.
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Evento</TableHead>
            <TableHead>Ação</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Quando</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((execution) => (
            <TableRow key={execution.id}>
              <TableCell className="text-sm">
                {eventLabel(execution.eventType)}
              </TableCell>
              <TableCell className="text-sm">
                {WORKFLOW_ACTION_LABELS[execution.actionType]}
              </TableCell>
              <TableCell>
                <StatusBadge status={execution.status} />
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {formatDate(execution.createdAt)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
