"use client";

import { useState } from "react";
import { DataTable } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Eye } from "lucide-react";
import { ListPagination } from "@/components/common/ListPagination";
import { AuditActionBadge } from "./AuditActionBadge";
import { AuditModuleBadge } from "./AuditModuleBadge";
import { AuditStatusBadge } from "./AuditStatusBadge";
import type { AuditLog, AuditLogPageResponse } from "../types/audit.types";

interface AuditTableProps {
  data: AuditLogPageResponse | undefined;
  isLoading: boolean;
  pageSize: number;
  /** Página 1-based (formato da API de auditoria). */
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onRowClick: (log: AuditLog) => void;
}

export function AuditTable({
  data,
  isLoading,
  pageSize,
  onPageChange,
  onPageSizeChange,
  onRowClick,
}: AuditTableProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const formatTime = (dateString: string) => {
    return new Date(dateString).toLocaleTimeString("pt-BR", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  if (isLoading) {
    return (
      <div className="flex h-32 items-center justify-center">
        <p className="text-muted-foreground">Carregando logs de auditoria...</p>
      </div>
    );
  }

  const logs = data?.content || [];
  const page = data?.page || 1;
  const totalPages = data?.totalPages || 0;

  return (
    <div className="space-y-4">
      <DataTable
        tableId="audit"
        data={logs ?? []}
        emptyState={{ title: "Nenhum log de auditoria encontrado." }}
        columns={[
          {
            id: "data-hora",
            header: "Data/Hora",
            className: "whitespace-nowrap",
            cell: (log) => (
              <>
                <div className="text-sm">{formatDate(log.createdAt)}</div>
                <div className="text-xs text-muted-foreground">{formatTime(log.createdAt)}</div>
              </>
            ),
          },
          {
            id: "usuario",
            header: "Usuário",
            cell: (log) => (
              <>
                <div className="text-sm font-medium">{log.userName || log.userEmail || "-"}</div>
                {log.userName && log.userEmail && (
                  <div className="text-xs text-muted-foreground">{log.userEmail}</div>
                )}
              </>
            ),
          },
          { id: "acao", header: "Ação", cell: (log) => <AuditActionBadge action={log.action} /> },
          {
            id: "modulo",
            header: "Módulo",
            cell: (log) => <AuditModuleBadge module={log.module} />,
          },
          {
            id: "entidade",
            header: "Entidade",
            className: "text-sm text-muted-foreground",
            cell: (log) => (
              <>
                {log.entityName
                  ? `${log.entityName}${log.entityId ? ` (${log.entityId.slice(0, 8)}...)` : ""}`
                  : "-"}
              </>
            ),
          },
          {
            id: "descricao",
            header: "Descrição",
            className: "max-w-[200px] truncate text-sm text-muted-foreground",
            cell: (log) => <>{log.description || "-"}</>,
          },
          {
            id: "status",
            header: "Status",
            cell: (log) => <AuditStatusBadge status={log.status} />,
          },
          {
            id: "acoes",
            header: "",
            label: "Ações",
            hideable: false,
            headerClassName: "w-[50px]",
            cell: (log) => (
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={(e) => {
                  e.stopPropagation();
                  onRowClick(log);
                }}
              >
                <Eye className="h-4 w-4" />
              </Button>
            ),
          },
        ]}
        keyExtractor={(log) => log.id}
        onRowClick={(log) => onRowClick(log)}
      />

      <ListPagination
        page={page - 1}
        pageSize={pageSize}
        totalPages={totalPages}
        totalElements={data?.totalElements ?? 0}
        onPageChange={(next) => onPageChange(next + 1)}
        onPageSizeChange={onPageSizeChange}
      />
    </div>
  );
}
