"use client";

import type { AuditModule } from "../types/audit.types";
import { Badge } from "@/components/ui/badge";

const moduleConfig: Record<string, { label: string; className?: string }> = {
  AUTH: {
    label: "Autenticação",
    className: "bg-blue-100 text-blue-800 border-blue-200",
  },
  TENANTS: {
    label: "Empresas",
    className: "bg-purple-100 text-purple-800 border-purple-200",
  },
  USERS: {
    label: "Usuários",
    className: "bg-green-100 text-green-800 border-green-200",
  },
  ROLES: {
    label: "Roles",
    className: "bg-yellow-100 text-yellow-800 border-yellow-200",
  },
  PERMISSIONS: {
    label: "Permissões",
    className: "bg-orange-100 text-orange-800 border-orange-200",
  },
  CUSTOMERS: {
    label: "Clientes",
    className: "bg-cyan-100 text-cyan-800 border-cyan-200",
  },
  CONTACTS: {
    label: "Contatos",
    className: "bg-teal-100 text-teal-800 border-teal-200",
  },
  LEADS: {
    label: "Leads",
    className: "bg-indigo-100 text-indigo-800 border-indigo-200",
  },
  PIPELINE: {
    label: "Pipeline",
    className: "bg-pink-100 text-pink-800 border-pink-200",
  },
  TASKS: {
    label: "Tarefas",
    className: "bg-amber-100 text-amber-800 border-amber-200",
  },
  CALENDAR: {
    label: "Calendário",
    className: "bg-lime-100 text-lime-800 border-lime-200",
  },
  FINANCE: {
    label: "Financeiro",
    className: "bg-emerald-100 text-emerald-800 border-emerald-200",
  },
  REPORTS: {
    label: "Relatórios",
    className: "bg-sky-100 text-sky-800 border-sky-200",
  },
  SETTINGS: {
    label: "Configurações",
    className: "bg-gray-100 text-gray-800 border-gray-200",
  },
  AUDIT: {
    label: "Auditoria",
    className: "bg-rose-100 text-rose-800 border-rose-200",
  },
  SYSTEM: {
    label: "Sistema",
    className: "bg-slate-100 text-slate-800 border-slate-200",
  },
};

interface AuditModuleBadgeProps {
  module: AuditModule;
}

export function AuditModuleBadge({ module }: AuditModuleBadgeProps) {
  const config = moduleConfig[module] || { label: module, className: "" };
  return (
    <Badge variant="outline" className={`text-xs ${config.className}`}>
      {config.label}
    </Badge>
  );
}
