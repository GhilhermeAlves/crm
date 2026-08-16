"use client";

import { useState } from "react";
import { Search, X, Calendar, Filter } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { AuditLogSearchParams } from "../types/audit.types";

interface AuditFiltersProps {
  params: AuditLogSearchParams;
  onParamsChange: (params: AuditLogSearchParams) => void;
}

const MODULES = [
  "AUTH",
  "TENANTS",
  "USERS",
  "ROLES",
  "PERMISSIONS",
  "CUSTOMERS",
  "CONTACTS",
  "LEADS",
  "PIPELINE",
  "TASKS",
  "CALENDAR",
  "FINANCE",
  "REPORTS",
  "SETTINGS",
  "AUDIT",
  "SYSTEM",
];

const ACTIONS = [
  "CREATE",
  "READ",
  "UPDATE",
  "DELETE",
  "LOGIN",
  "LOGOUT",
  "EXPORT",
  "IMPORT",
  "APPROVE",
  "REJECT",
  "ASSIGN",
  "UNASSIGN",
  "RESET_PASSWORD",
  "CHANGE_PASSWORD",
  "GENERATE_REPORT",
];

const STATUSES = ["SUCCESS", "FAILED", "ERROR"];

const MODULE_LABELS: Record<string, string> = {
  AUTH: "Autenticação",
  TENANTS: "Empresas",
  USERS: "Usuários",
  ROLES: "Roles",
  PERMISSIONS: "Permissões",
  CUSTOMERS: "Clientes",
  CONTACTS: "Contatos",
  LEADS: "Leads",
  PIPELINE: "Pipeline",
  TASKS: "Tarefas",
  CALENDAR: "Calendário",
  FINANCE: "Financeiro",
  REPORTS: "Relatórios",
  SETTINGS: "Configurações",
  AUDIT: "Auditoria",
  SYSTEM: "Sistema",
};

const ACTION_LABELS: Record<string, string> = {
  CREATE: "Criar",
  READ: "Ler",
  UPDATE: "Atualizar",
  DELETE: "Excluir",
  LOGIN: "Login",
  LOGOUT: "Logout",
  EXPORT: "Exportar",
  IMPORT: "Importar",
  APPROVE: "Aprovar",
  REJECT: "Rejeitar",
  ASSIGN: "Atribuir",
  UNASSIGN: "Desatribuir",
  RESET_PASSWORD: "Reset Senha",
  CHANGE_PASSWORD: "Alterar Senha",
  GENERATE_REPORT: "Gerar Relatório",
};

const STATUS_LABELS: Record<string, string> = {
  SUCCESS: "Sucesso",
  FAILED: "Falhou",
  ERROR: "Erro",
};

export function AuditFilters({ params, onParamsChange }: AuditFiltersProps) {
  const [searchInput, setSearchInput] = useState(params.search || "");

  const handleSearch = () => {
    onParamsChange({ ...params, search: searchInput, page: 1 });
  };

  const handleClear = () => {
    setSearchInput("");
    onParamsChange({ page: 1, pageSize: params.pageSize });
  };

  const hasActiveFilters =
    params.module ||
    params.action ||
    params.status ||
    params.search ||
    params.startDate ||
    params.endDate;

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 flex-wrap">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar por descrição, usuário, URI..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            className="pl-9"
          />
        </div>
        <Button variant="outline" onClick={handleSearch}>
          Buscar
        </Button>
        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={handleClear}>
            <X className="h-4 w-4 mr-1" />
            Limpar
          </Button>
        )}
      </div>

      <div className="flex items-center gap-2 flex-wrap">
        <Filter className="h-4 w-4 text-muted-foreground" />

        <Select
          value={params.module || ""}
          onValueChange={(value) =>
            onParamsChange({ ...params, module: value, page: 1 })
          }
        >
          <SelectTrigger className="w-[150px]">
            <SelectValue placeholder="Módulo" />
          </SelectTrigger>
          <SelectContent>
            {MODULES.map((m) => (
              <SelectItem key={m} value={m}>
                {MODULE_LABELS[m] || m}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={params.action || ""}
          onValueChange={(value) =>
            onParamsChange({ ...params, action: value, page: 1 })
          }
        >
          <SelectTrigger className="w-[150px]">
            <SelectValue placeholder="Ação" />
          </SelectTrigger>
          <SelectContent>
            {ACTIONS.map((a) => (
              <SelectItem key={a} value={a}>
                {ACTION_LABELS[a] || a}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={params.status || ""}
          onValueChange={(value) =>
            onParamsChange({ ...params, status: value, page: 1 })
          }
        >
          <SelectTrigger className="w-[130px]">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            {STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {STATUS_LABELS[s] || s}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex items-center gap-1">
          <Calendar className="h-4 w-4 text-muted-foreground" />
          <Input
            type="datetime-local"
            value={params.startDate ? params.startDate.slice(0, 16) : ""}
            onChange={(e) =>
              onParamsChange({
                ...params,
                startDate: e.target.value
                  ? new Date(e.target.value).toISOString()
                  : undefined,
                page: 1,
              })
            }
            className="w-[200px]"
          />
          <span className="text-muted-foreground">até</span>
          <Input
            type="datetime-local"
            value={params.endDate ? params.endDate.slice(0, 16) : ""}
            onChange={(e) =>
              onParamsChange({
                ...params,
                endDate: e.target.value
                  ? new Date(e.target.value).toISOString()
                  : undefined,
                page: 1,
              })
            }
            className="w-[200px]"
          />
        </div>
      </div>
    </div>
  );
}
