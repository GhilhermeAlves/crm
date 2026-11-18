"use client";

import { useRouter } from "next/navigation";
import { MoreHorizontal, Eye, Pencil, Trash2, Workflow as WorkflowIcon } from "lucide-react";
import type { Workflow } from "../types/workflow.types";
import { WORKFLOW_TRIGGER_LABELS } from "../types/workflow.types";
import { DataTable } from "@/components/ui/data-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Switch } from "@/components/ui/switch";
import { ROUTES } from "@/lib/constants";

interface WorkflowTableProps {
  workflows: Workflow[];
  isLoading?: boolean;
  canUpdate?: boolean;
  canDelete?: boolean;
  busyId?: string | null;
  onToggle?: (workflow: Workflow) => void;
  onDelete?: (workflow: Workflow) => void;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function WorkflowTable({
  workflows,
  isLoading,
  canUpdate = true,
  canDelete = true,
  busyId,
  onToggle,
  onDelete,
}: WorkflowTableProps) {
  const router = useRouter();

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-14 animate-pulse rounded bg-muted" />
        ))}
      </div>
    );
  }

  if (workflows.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
        <WorkflowIcon className="mb-4 h-12 w-12 opacity-50" />
        <p className="text-lg font-medium">Nenhum workflow configurado</p>
        <p className="text-sm">Crie automações para seguir vendas e tarefas automaticamente.</p>
      </div>
    );
  }

  return (
    <DataTable
      tableId="workflows"
      data={workflows ?? []}
      columns={[
        {
          id: "nome",
          header: "Nome",
          cell: (workflow) => (
            <>
              <button
                type="button"
                onClick={() => router.push(`${ROUTES.WORKFLOWS}/${workflow.id}`)}
                className="text-left font-medium hover:underline"
              >
                {workflow.name}
              </button>
              {workflow.description && (
                <div className="line-clamp-1 text-xs text-muted-foreground">
                  {workflow.description}
                </div>
              )}
            </>
          ),
        },
        {
          id: "disparo",
          header: "Disparo",
          className: "text-sm",
          cell: (workflow) => <>{WORKFLOW_TRIGGER_LABELS[workflow.trigger]}</>,
        },
        {
          id: "ativo",
          header: "Ativo",
          cell: (workflow) => (
            <>
              {canUpdate ? (
                <Switch
                  checked={workflow.active}
                  disabled={busyId === workflow.id}
                  onCheckedChange={() => onToggle?.(workflow)}
                  aria-label={workflow.active ? "Desativar" : "Ativar"}
                />
              ) : (
                <Badge variant={workflow.active ? "default" : "outline"}>
                  {workflow.active ? "Ativo" : "Inativo"}
                </Badge>
              )}
            </>
          ),
        },
        {
          id: "atualizado-em",
          header: "Atualizado em",
          className: "text-sm text-muted-foreground",
          sortValue: (workflow) => workflow.updatedAt,
          cell: (workflow) => <>{formatDate(workflow.updatedAt)}</>,
        },
        {
          id: "acoes",
          header: "",
          label: "Ações",
          hideable: false,
          headerClassName: "w-[80px]",
          cell: (workflow) => (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => router.push(`${ROUTES.WORKFLOWS}/${workflow.id}`)}>
                  <Eye className="mr-2 h-4 w-4" />
                  Visualizar
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={() => router.push(`${ROUTES.WORKFLOWS}/${workflow.id}/edit`)}
                >
                  <Pencil className="mr-2 h-4 w-4" />
                  Editar
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                {canDelete && (
                  <DropdownMenuItem
                    onClick={() => onDelete?.(workflow)}
                    className="text-destructive"
                  >
                    <Trash2 className="mr-2 h-4 w-4" />
                    Excluir
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          ),
        },
      ]}
      keyExtractor={(workflow) => workflow.id}
    />
  );
}
