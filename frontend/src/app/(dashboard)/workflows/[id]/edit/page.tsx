"use client";

import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useWorkflow, useUpdateWorkflow } from "@/features/workflows/hooks/useWorkflows";
import { WorkflowForm } from "@/features/workflows/components/WorkflowForm";
import { useWorkflowPermissions } from "@/features/workflows/schemas/workflow.schema";
import { ROUTES } from "@/lib/constants";

export default function EditWorkflowPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const workflowId = params?.id ?? null;
  const { canUpdate } = useWorkflowPermissions();

  const { data: workflow, isLoading } = useWorkflow(companyId, workflowId);
  const updateWorkflow = useUpdateWorkflow(companyId);

  if (!canUpdate) {
    return (
      <div className="space-y-6">
        <PageTitle>Sem permissão</PageTitle>
        <p className="text-muted-foreground">Você não tem permissão para editar workflows.</p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-64 animate-pulse rounded bg-muted" />
        <div className="h-72 animate-pulse rounded bg-muted" />
      </div>
    );
  }

  if (!workflow) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="icon" onClick={() => router.push(ROUTES.WORKFLOWS)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <PageTitle>Workflow não encontrado</PageTitle>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => router.push(ROUTES.WORKFLOWS)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <PageTitle>Editar workflow</PageTitle>
      </div>

      <Card>
        <CardContent className="pt-6">
          <WorkflowForm
            initial={workflow}
            isLoading={updateWorkflow.isPending}
            submitLabel="Salvar alterações"
            onSubmit={(payload) =>
              updateWorkflow.mutate(
                { id: workflowId as string, data: payload },
                {
                  onSuccess: () => router.push(`${ROUTES.WORKFLOWS}/${workflowId}`),
                },
              )
            }
          />
        </CardContent>
      </Card>
    </div>
  );
}
