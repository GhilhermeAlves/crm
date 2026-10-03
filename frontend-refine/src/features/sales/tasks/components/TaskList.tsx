"use client";

import { useState } from "react";
import type { Task, TaskStatus } from "../types/task.types";
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS } from "../types/task.types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Check, RefreshCw, Play, X, Loader2 } from "lucide-react";

type Props = {
  tasks: Task[];
  isLoading?: boolean;
  canUpdate: boolean;
  busyId?: string | null;
  onChangeStatus: (id: string, status: TaskStatus) => void;
};

const priorityBadge: Record<string, "secondary" | "default" | "destructive"> = {
  LOW: "secondary",
  MEDIUM: "default",
  HIGH: "destructive",
};

export function TaskList({ tasks, isLoading, canUpdate, busyId, onChangeStatus }: Props) {
  const [expandedId, setExpandedId] = useState<string | null>(null);

  if (isLoading) {
    return <p className="py-6 text-center text-sm text-muted-foreground">Carregando…</p>;
  }

  if (tasks.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">Nenhuma tarefa por aqui.</p>
    );
  }

  const sorted = [...tasks].sort((a, b) => {
    if (a.status === "COMPLETED" && b.status !== "COMPLETED") return 1;
    if (b.status === "COMPLETED" && a.status !== "COMPLETED") return -1;
    const pa = a.priority === "HIGH" ? 3 : a.priority === "MEDIUM" ? 2 : 1;
    const pb = b.priority === "HIGH" ? 3 : b.priority === "MEDIUM" ? 2 : 1;
    return pb - pa;
  });

  return (
    <ul className="divide-y divide-border">
      {sorted.map((task) => {
        const busy = busyId === task.id;
        return (
          <li key={task.id} className="py-3">
            <div className="flex items-start justify-between gap-2">
              <button
                type="button"
                className="flex-1 text-left"
                onClick={() => setExpandedId(expandedId === task.id ? null : task.id)}
              >
                <span
                  className={`text-sm font-medium ${
                    task.status === "COMPLETED" ? "text-muted-foreground line-through" : ""
                  }`}
                >
                  {task.title}
                </span>
                <div className="mt-1 flex flex-wrap items-center gap-2">
                  <Badge variant={priorityBadge[task.priority]}>
                    {TASK_PRIORITY_LABELS[task.priority]}
                  </Badge>
                  <Badge variant="outline">{TASK_STATUS_LABELS[task.status]}</Badge>
                  {task.dueAt && (
                    <span className="text-xs text-muted-foreground">
                      Vence {new Date(task.dueAt).toLocaleDateString("pt-BR")}
                    </span>
                  )}
                </div>
              </button>

              {canUpdate && task.status !== "COMPLETED" && task.status !== "CANCELLED" && (
                <div className="flex shrink-0 items-center gap-1">
                  {task.status === "PENDING" && (
                    <Button
                      size="icon"
                      variant="ghost"
                      title="Iniciar"
                      disabled={busy}
                      onClick={() => onChangeStatus(task.id, "IN_PROGRESS")}
                    >
                      {busy ? <Loader2 className="animate-spin" /> : <Play />}
                    </Button>
                  )}
                  <Button
                    size="icon"
                    variant="ghost"
                    title="Concluir"
                    disabled={busy}
                    onClick={() => onChangeStatus(task.id, "COMPLETED")}
                  >
                    {busy ? <Loader2 className="animate-spin" /> : <Check />}
                  </Button>
                  {task.status === "PENDING" && (
                    <Button
                      size="icon"
                      variant="ghost"
                      title="Cancelar"
                      disabled={busy}
                      onClick={() => onChangeStatus(task.id, "CANCELLED")}
                    >
                      <X />
                    </Button>
                  )}
                </div>
              )}

              {canUpdate && task.status === "COMPLETED" && (
                <Button
                  size="icon"
                  variant="ghost"
                  title="Reabrir"
                  disabled={busy}
                  onClick={() => onChangeStatus(task.id, "PENDING")}
                >
                  {busy ? <Loader2 className="animate-spin" /> : <RefreshCw />}
                </Button>
              )}
            </div>

            {expandedId === task.id && task.description && (
              <p className="mt-2 pl-1 text-sm text-muted-foreground">{task.description}</p>
            )}
          </li>
        );
      })}
    </ul>
  );
}
