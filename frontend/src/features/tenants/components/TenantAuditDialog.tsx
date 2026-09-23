"use client";

import { useQuery } from "@tanstack/react-query";
import { History } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AuditActionBadge } from "@/features/audit/components/AuditActionBadge";
import { AuditStatusBadge } from "@/features/audit/components/AuditStatusBadge";
import { AuditService } from "@/features/audit/services/audit.service";
import type { Tenant } from "../types/tenant.types";

type TenantAuditDialogProps = {
  tenant: Tenant | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export function TenantAuditDialog({ tenant, open, onOpenChange }: TenantAuditDialogProps) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["tenant-audit", tenant?.id],
    queryFn: () =>
      AuditService.search({
        module: "TENANTS",
        entityId: tenant!.id,
        entityName: "Company",
        page: 1,
        pageSize: 50,
      }),
    enabled: open && !!tenant,
  });

  const formatDateTime = (dateString: string) =>
    new Date(dateString).toLocaleString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  const logs = data?.content ?? [];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[80vh] overflow-y-auto sm:max-w-xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <History className="h-5 w-5" />
            Auditoria — {tenant?.tradingName ?? "Empresa"}
          </DialogTitle>
          <DialogDescription>Histórico de criação e atualizações desta empresa.</DialogDescription>
        </DialogHeader>

        {isLoading && (
          <p className="py-8 text-center text-sm text-muted-foreground">Carregando histórico...</p>
        )}

        {isError && (
          <p className="py-8 text-center text-sm text-destructive">
            Não foi possível carregar o histórico de auditoria.
          </p>
        )}

        {!isLoading && !isError && logs.length === 0 && (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Nenhum evento de auditoria encontrado para esta empresa.
          </p>
        )}

        {!isLoading && !isError && logs.length > 0 && (
          <div className="space-y-4 pb-2">
            {logs.map((log) => (
              <div key={log.id} className="space-y-2 rounded-md border p-3">
                <div className="flex items-center justify-between gap-2">
                  <AuditActionBadge action={log.action} />
                  <AuditStatusBadge status={log.status} />
                </div>
                <p className="text-sm">{log.description || "Sem descrição"}</p>
                <div className="flex items-center justify-between gap-2 text-xs text-muted-foreground">
                  <span className="truncate">{log.userName || log.userEmail || "Sistema"}</span>
                  <span className="shrink-0">{formatDateTime(log.createdAt)}</span>
                </div>
                {log.oldValues && Object.keys(log.oldValues).length > 0 && (
                  <div>
                    <p className="mb-1 text-xs font-medium text-muted-foreground">
                      Valores anteriores
                    </p>
                    <FormattedValues values={log.oldValues} />
                  </div>
                )}
                {log.newValues && Object.keys(log.newValues).length > 0 && (
                  <div>
                    <p className="mb-1 text-xs font-medium text-muted-foreground">Valores</p>
                    <FormattedValues values={log.newValues} />
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function FormattedValues({ values }: { values: Record<string, unknown> }) {
  return (
    <div className="space-y-1 rounded-md border bg-muted/30 p-3 text-xs">
      {Object.entries(values).map(([key, value]) => (
        <div key={key} className="flex gap-2">
          <span className="w-32 shrink-0 truncate font-medium text-muted-foreground">{key}</span>
          <span className="break-all">{value === null || value === undefined ? "—" : String(value)}</span>
        </div>
      ))}
    </div>
  );
}