"use client";

import type { AuditStatus } from "../types/audit.types";
import { CheckCircle, XCircle, AlertTriangle } from "lucide-react";
import { cn } from "@/lib/utils";

const statusConfig: Record<
  string,
  {
    label: string;
    icon: React.ComponentType<{ className?: string }>;
    className: string;
  }
> = {
  SUCCESS: { label: "Sucesso", icon: CheckCircle, className: "text-green-600" },
  FAILED: { label: "Falhou", icon: XCircle, className: "text-red-600" },
  ERROR: { label: "Erro", icon: AlertTriangle, className: "text-amber-600" },
};

interface AuditStatusBadgeProps {
  status: AuditStatus;
}

export function AuditStatusBadge({ status }: AuditStatusBadgeProps) {
  const config = statusConfig[status] || {
    label: status,
    icon: AlertTriangle,
    className: "text-gray-600",
  };
  const Icon = config.icon;

  return (
    <span className={cn("inline-flex items-center gap-1 text-sm font-medium", config.className)}>
      <Icon className="h-3.5 w-3.5" />
      {config.label}
    </span>
  );
}
