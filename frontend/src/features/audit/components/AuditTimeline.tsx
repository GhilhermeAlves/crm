"use client";

import type { AuditLog } from "../types/audit.types";
import { AuditActionBadge } from "./AuditActionBadge";
import { AuditModuleBadge } from "./AuditModuleBadge";
import { AuditStatusBadge } from "./AuditStatusBadge";

interface AuditTimelineProps {
  logs: AuditLog[];
}

export function AuditTimeline({ logs }: AuditTimelineProps) {
  const formatTime = (dateString: string) => {
    return new Date(dateString).toLocaleTimeString("pt-BR", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  if (logs.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        Nenhum evento para exibir na timeline
      </div>
    );
  }

  return (
    <div className="relative">
      <div className="absolute left-4 top-0 bottom-0 w-px bg-border" />
      <div className="space-y-4">
        {logs.map((log) => (
          <div key={log.id} className="relative pl-10">
            <div className="absolute left-2.5 top-1.5 h-3 w-3 rounded-full border-2 border-background bg-primary" />
            <div className="rounded-md border p-3 space-y-1">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AuditActionBadge action={log.action} />
                  <AuditModuleBadge module={log.module} />
                </div>
                <AuditStatusBadge status={log.status} />
              </div>
              <p className="text-sm">{log.description || "Sem descrição"}</p>
              <div className="flex items-center gap-4 text-xs text-muted-foreground">
                <span>{log.userName || log.userEmail || "Sistema"}</span>
                <span>{formatDate(log.createdAt)} {formatTime(log.createdAt)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
