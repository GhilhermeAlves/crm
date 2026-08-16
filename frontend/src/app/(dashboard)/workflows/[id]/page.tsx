"use client";

import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Pencil, Power } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  useWorkflow,
  useWorkflowExecutions,
  useToggleWorkflow,
} from "@/features/workflows/hooks/useWorkflows";
import { WorkflowExecutionsPanel } from "@/features/workflows/components/WorkflowExecutionsPanel";
import { useWorkflowPermissions } from "@/features/workflows/schemas/workflow.schema";
import {
  WORKFLOW_TRIGGER_LABELS,
  WORKFLOW_ACTION_LABELS,
  CONDITION_OPERATOR_LABELS,
} from "@/features/workflows/types/workflow.types";
import { ROUTES } from "@/lib/constants";

export default function WorkflowDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const workflowId = params?.id ?? null;
  const perms = useWorkflowPermissions();

  const { data: workflow, isLoading } = useWorkflow(companyId, workflowId);
  const { data: executions = [], isLoading: executionsLoading } =
    useWorkflowExecutions(companyId, workflowId);
  const toggle = useToggleWorkflow(companyId);

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-64 animate-pulse rounded bg-muted" />
        <div className="h-40 animate-pulse rounded bg-muted" />
      </div>
    );
  }

  if (!workflow) {
    return (
      <div className="space-y-6">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push(ROUTES.WORKFLOWS)}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <PageTitle>Workflow não encontrado</PageTitle>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.push(ROUTES.WORKFLOWS)}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <div className="flex items-center gap-2">
              <PageTitle>{workflow.name}</PageTitle>
              <Badge variant={workflow.active ? "default" : "outline"}>
                {workflow.active ? "Ativo" : "Inativo"}
              </Badge>
            </div>
            {workflow.description && (
              <p className="text-sm text-muted-foreground">
                {workflow.description}
              </p>
            )}
          </div>
        </div>
        <div className="flex gap-2">
          {perms.canUpdate && (
            <>
              <Button
                variant="outline"
                onClick={() =>
                  toggle.mutate({ id: workflow.id, active: workflow.active })
                }
              >
                <Power className="mr-2 h-4 w-4" />
                {workflow.active ? "Desativar" : "Ativar"}
              </Button>
              <Button
                onClick={() =>
                  router.push(`${ROUTES.WORKFLOWS}/${workflow.id}/edit`)
                }
              >
                <Pencil className="mr-2 h-4 w-4" />
                Editar
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">Detalhes</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div>
              <span className="font-medium">Disparo: </span>
              {WORKFLOW_TRIGGER_LABELS[workflow.trigger]}
            </div>
            <div>
              <span className="font-medium">Ações: </span>
              {workflow.actions
                .map((a) => WORKFLOW_ACTION_LABELS[a.actionType])
                .join(", ") || "—"}
            </div>
            <div>
              <span className="font-medium">Condições: </span>
              {workflow.conditions.length > 0
                ? workflow.conditions.map((c) => c.field).join(", ")
                : "Sem condições"}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">Regras</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {workflow.conditions.length === 0 ? (
              <p className="text-muted-foreground">
                Sem condições — executa em todo disparo.
              </p>
            ) : (
              workflow.conditions.map((c, i) => (
                <div key={c.id ?? i} className="flex items-center gap-2">
                  <code className="rounded bg-muted px-1.5 py-0.5 text-xs">
                    {c.field}
                  </code>
                  <span>—</span>
                  <span>{CONDITION_OPERATOR_LABELS[c.operator]}</span>
                  <code className="rounded bg-muted px-1.5 py-0.5 text-xs">
                    {c.value}
                  </code>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">
            Histórico de execuções
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <WorkflowExecutionsPanel
            data={executions}
            isLoading={executionsLoading}
          />
        </CardContent>
      </Card>
    </div>
  );
}
