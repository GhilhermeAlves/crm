"use client";

import type { AuditAction } from "../types/audit.types";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const actionConfig: Record<
  string,
  {
    label: string;
    variant: "default" | "secondary" | "destructive" | "outline";
  }
> = {
  CREATE: { label: "Criar", variant: "default" },
  READ: { label: "Ler", variant: "secondary" },
  UPDATE: { label: "Atualizar", variant: "default" },
  DELETE: { label: "Excluir", variant: "destructive" },
  LOGIN: { label: "Login", variant: "default" },
  LOGOUT: { label: "Logout", variant: "secondary" },
  EXPORT: { label: "Exportar", variant: "outline" },
  IMPORT: { label: "Importar", variant: "outline" },
  APPROVE: { label: "Aprovar", variant: "default" },
  REJECT: { label: "Rejeitar", variant: "destructive" },
  ASSIGN: { label: "Atribuir", variant: "default" },
  UNASSIGN: { label: "Desatribuir", variant: "secondary" },
  RESET_PASSWORD: { label: "Reset Senha", variant: "destructive" },
  CHANGE_PASSWORD: { label: "Alterar Senha", variant: "default" },
  GENERATE_REPORT: { label: "Relatório", variant: "outline" },
  CUSTOM: { label: "Personalizado", variant: "secondary" },
};

interface AuditActionBadgeProps {
  action: AuditAction;
}

export function AuditActionBadge({ action }: AuditActionBadgeProps) {
  const config = actionConfig[action] || {
    label: action,
    variant: "secondary" as const,
  };
  return (
    <Badge variant={config.variant} className="text-xs">
      {config.label}
    </Badge>
  );
}
