"use client";

import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useCreateWorkflow } from "@/features/workflows/hooks/useWorkflows";
import { WorkflowForm } from "@/features/workflows/components/WorkflowForm";
import { useWorkflowPermissions } from "@/features/workflows/schemas/workflow.schema";
import { ROUTES } from "@/lib/constants";

export default function NewWorkflowPage() {
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { canCreate } = useWorkflowPermissions();
  const createWorkflow = useCreateWorkflow(companyId);

  if (!canCreate) {
    return (
      <div className="space-y-6">
        <PageTitle>Sem permissão</PageTitle>
        <p className="text-muted-foreground">
          Você não tem permissão para criar workflows.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push(ROUTES.WORKFLOWS)}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <PageTitle>Novo workflow</PageTitle>
      </div>

      <Card>
        <CardContent className="pt-6">
          <WorkflowForm
            isLoading={createWorkflow.isPending}
            submitLabel="Criar workflow"
            onSubmit={(payload) =>
              createWorkflow.mutate(payload, {
                onSuccess: () => router.push(ROUTES.WORKFLOWS),
              })
            }
          />
        </CardContent>
      </Card>
    </div>
  );
}
