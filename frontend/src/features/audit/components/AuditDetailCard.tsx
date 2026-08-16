"use client";

import type { AuditLog } from "../types/audit.types";
import { AuditActionBadge } from "./AuditActionBadge";
import { AuditModuleBadge } from "./AuditModuleBadge";
import { AuditStatusBadge } from "./AuditStatusBadge";
import { JsonViewer } from "./JsonViewer";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { ArrowLeft, Copy, Check } from "lucide-react";
import { useState } from "react";
import Link from "next/link";

interface AuditDetailCardProps {
  log: AuditLog;
}

export function AuditDetailCard({ log }: AuditDetailCardProps) {
  const [copied, setCopied] = useState(false);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const handleCopyId = () => {
    navigator.clipboard.writeText(log.id);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/audit">
            <ArrowLeft className="h-4 w-4 mr-1" />
            Voltar
          </Link>
        </Button>
      </div>

      <div className="rounded-lg border p-6 space-y-6">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <h2 className="text-xl font-bold">Detalhes do Log de Auditoria</h2>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span>ID: {log.id}</span>
              <Button
                variant="ghost"
                size="icon"
                className="h-5 w-5"
                onClick={handleCopyId}
              >
                {copied ? (
                  <Check className="h-3 w-3" />
                ) : (
                  <Copy className="h-3 w-3" />
                )}
              </Button>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <AuditStatusBadge status={log.status} />
          </div>
        </div>

        <Separator />

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">
              Data/Hora
            </p>
            <p className="text-sm">{formatDate(log.createdAt)}</p>
          </div>
          <div>
            <p className="text-sm font-medium text-muted-foreground">Usuário</p>
            <p className="text-sm">
              {log.userName || log.userEmail || "Sistema"}
            </p>
          </div>
          <div>
            <p className="text-sm font-medium text-muted-foreground">Email</p>
            <p className="text-sm">{log.userEmail || "-"}</p>
          </div>
          <div>
            <p className="text-sm font-medium text-muted-foreground">Ação</p>
            <AuditActionBadge action={log.action} />
          </div>
          <div>
            <p className="text-sm font-medium text-muted-foreground">Módulo</p>
            <AuditModuleBadge module={log.module} />
          </div>
          <div>
            <p className="text-sm font-medium text-muted-foreground">
              Entidade
            </p>
            <p className="text-sm">
              {log.entityName || "-"}
              {log.entityId ? ` (${log.entityId})` : ""}
            </p>
          </div>
          <div className="lg:col-span-3">
            <p className="text-sm font-medium text-muted-foreground">
              Descrição
            </p>
            <p className="text-sm">{log.description || "-"}</p>
          </div>
        </div>

        <Separator />

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <p className="text-sm font-medium text-muted-foreground">IP</p>
            <p className="text-sm font-mono">{log.ipAddress || "-"}</p>
          </div>
          <div>
            <p className="text-sm font-medium text-muted-foreground">Método</p>
            <p className="text-sm">{log.requestMethod || "-"}</p>
          </div>
          <div>
            <p className="text-sm font-medium text-muted-foreground">URI</p>
            <p className="text-sm font-mono text-xs break-all">
              {log.requestUri || "-"}
            </p>
          </div>
          <div className="lg:col-span-3">
            <p className="text-sm font-medium text-muted-foreground">
              User Agent
            </p>
            <p className="text-xs font-mono text-muted-foreground break-all">
              {log.userAgent || "-"}
            </p>
          </div>
        </div>

        {(log.oldValues || log.newValues) && (
          <>
            <Separator />
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {log.oldValues && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground mb-2">
                    Valores Anteriores
                  </p>
                  <JsonViewer data={log.oldValues} />
                </div>
              )}
              {log.newValues && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground mb-2">
                    Valores Novos
                  </p>
                  <JsonViewer data={log.newValues} />
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
