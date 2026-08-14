"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  useWorkflows,
  useToggleWorkflow,
  useDeleteWorkflow,
} from "@/features/workflows/hooks/useWorkflows";
import { WorkflowTable } from "@/features/workflows/components/WorkflowTable";
import { DeleteWorkflowDialog } from "@/features/workflows/components/DeleteWorkflowDialog";
import { useWorkflowPermissions } from "@/features/workflows/schemas/workflow.schema";
import type { Workflow } from "@/features/workflows/types/workflow.types";
import { ROUTES } from "@/lib/constants";

export default function WorkflowsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const perms = useWorkflowPermissions();

  const [deleteTarget, setDeleteTarget] = useState<Workflow | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const { data: workflows = [], isLoading } = useWorkflows(companyId);
  const toggle = useToggleWorkflow(companyId);
  const deleteWorkflow = useDeleteWorkflow(companyId);

  const handleToggle = (workflow: Workflow) => {
    setBusyId(workflow.id);
    toggle.mutate(
      { id: workflow.id, active: workflow.active },
      { onSettled: () => setBusyId(null) }
    );
  };

  const handleDelete = () => {
    if (!deleteTarget) return;
    deleteWorkflow.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <PageTitle>Workflows</PageTitle>
          <p className="text-sm text-muted-foreground">
            Automatize tarefas e atividades a partir de eventos do CRM.
          </p>
        </div>
        {perms.canCreate && (
          <Button onClick={() => router.push(`${ROUTES.WORKFLOWS}/new`)}>
            <Plus className="mr-2 h-4 w-4" />
            Novo Workflow
          </Button>
        )}
      </div>

      <Card>
        <CardContent className="p-0">
          <WorkflowTable
            workflows={workflows}
            isLoading={isLoading}
            canUpdate={perms.canUpdate}
            canDelete={perms.canDelete}
            busyId={busyId}
            onToggle={handleToggle}
            onDelete={setDeleteTarget}
          />
        </CardContent>
      </Card>

      <DeleteWorkflowDialog
        workflow={deleteTarget}
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        onConfirm={handleDelete}
        isLoading={deleteWorkflow.isPending}
      />
    </div>
  );
}