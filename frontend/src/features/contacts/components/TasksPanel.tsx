"use client";

import { CheckCircle2, Clock } from "lucide-react";
import type { TaskItem } from "../types/contact.types";
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS } from "../types/contact.types";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const formatDate = (iso: string): string =>
  new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" }).format(new Date(iso));

export function TasksPanel({ tasks }: { tasks: TaskItem[] }) {
  if (tasks.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Nenhuma tarefa associada a este contato.
      </p>
    );
  }

  return (
    <ul className="divide-y divide-border">
      {tasks.map((task) => {
        const done = task.status === "COMPLETED";
        return (
          <li key={task.id} className="flex items-start justify-between gap-3 py-3">
            <div className="min-w-0 space-y-1">
              <p
                className={cn(
                  "truncate text-sm font-medium",
                  done && "text-muted-foreground line-through",
                )}
              >
                {task.title}
              </p>
              <p className="flex items-center gap-3 text-xs text-muted-foreground">
                <span>{TASK_STATUS_LABELS[task.status]}</span>
                {task.priority && <span>{TASK_PRIORITY_LABELS[task.priority]}</span>}
              </p>
            </div>
            <div className="flex shrink-0 flex-col items-end gap-1">
              {task.dueAt && !done && (
                <span
                  className={cn(
                    "flex items-center gap-1 text-xs",
                    task.overdue ? "text-destructive" : "text-muted-foreground",
                  )}
                >
                  <Clock className="h-3 w-3" />
                  {formatDate(task.dueAt)}
                </span>
              )}
              {task.overdue && <Badge variant="destructive">Vencida</Badge>}
              {done && <CheckCircle2 className="h-4 w-4 text-emerald-500" />}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
