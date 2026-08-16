"use client";

import { useState } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Eye, ChevronLeft, ChevronRight } from "lucide-react";
import { AuditActionBadge } from "./AuditActionBadge";
import { AuditModuleBadge } from "./AuditModuleBadge";
import { AuditStatusBadge } from "./AuditStatusBadge";
import type { AuditLog, AuditLogPageResponse } from "../types/audit.types";

interface AuditTableProps {
  data: AuditLogPageResponse | undefined;
  isLoading: boolean;
  onPageChange: (page: number) => void;
  onRowClick: (log: AuditLog) => void;
}

export function AuditTable({
  data,
  isLoading,
  onPageChange,
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
      <div className="flex items-center justify-center h-32">
        <p className="text-muted-foreground">Carregando logs de auditoria...</p>
      </div>
    );
  }

  const logs = data?.content || [];
  const page = data?.page || 1;
  const totalPages = data?.totalPages || 0;

  return (
    <div className="space-y-4">
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Data/Hora</TableHead>
              <TableHead>Usuário</TableHead>
              <TableHead>Ação</TableHead>
              <TableHead>Módulo</TableHead>
              <TableHead>Entidade</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-[50px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {logs.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={8}
                  className="h-24 text-center text-muted-foreground"
                >
                  Nenhum log de auditoria encontrado.
                </TableCell>
              </TableRow>
            ) : (
              logs.map((log) => (
                <TableRow
                  key={log.id}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => onRowClick(log)}
                >
                  <TableCell className="whitespace-nowrap">
                    <div className="text-sm">{formatDate(log.createdAt)}</div>
                    <div className="text-xs text-muted-foreground">
                      {formatTime(log.createdAt)}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm font-medium">
                      {log.userName || log.userEmail || "-"}
                    </div>
                    {log.userName && log.userEmail && (
                      <div className="text-xs text-muted-foreground">
                        {log.userEmail}
                      </div>
                    )}
                  </TableCell>
                  <TableCell>
                    <AuditActionBadge action={log.action} />
                  </TableCell>
                  <TableCell>
                    <AuditModuleBadge module={log.module} />
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {log.entityName
                      ? `${log.entityName}${log.entityId ? ` (${log.entityId.slice(0, 8)}...)` : ""}`
                      : "-"}
                  </TableCell>
                  <TableCell className="max-w-[200px] truncate text-sm text-muted-foreground">
                    {log.description || "-"}
                  </TableCell>
                  <TableCell>
                    <AuditStatusBadge status={log.status} />
                  </TableCell>
                  <TableCell>
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
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Página {page} de {totalPages} ({data?.totalElements || 0} registros)
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(page - 1)}
              disabled={page <= 1}
            >
              <ChevronLeft className="h-4 w-4" />
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages}
            >
              Próxima
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
