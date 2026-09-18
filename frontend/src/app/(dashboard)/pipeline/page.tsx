"use client";

import { useMemo, useState } from "react";
import { Plus, ShieldOff, Target } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { usePipelines } from "@/features/pipeline/hooks/usePipelines";
import {
  useCreateOpportunity,
  useDeleteOpportunity,
  useMarkLostOpportunity,
  useMarkWonOpportunity,
  useMoveOpportunity,
  useOpportunities,
} from "@/features/pipeline/hooks/useOpportunities";
import { useOpportunityPermissions } from "@/features/pipeline/schemas/pipeline.schema";
import type { Opportunity } from "@/features/pipeline/types/pipeline.types";
import { PipelineBoard } from "@/features/pipeline/components/PipelineBoard";
import { CreateOpportunityDialog } from "@/features/pipeline/components/CreateOpportunityDialog";
import { LostReasonDialog } from "@/features/pipeline/components/LostReasonDialog";
import { PageTitle } from "@/components/common/PageTitle";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export default function PipelinePage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;
  const { canCreate, canMove, canWin, canLose, canDelete } = useOpportunityPermissions();

  const {
    data: pipelines,
    isLoading: pipelinesLoading,
    error: pipelinesError,
    refetch: refetchPipelines,
  } = usePipelines(companyId);

  const activePipelines = useMemo(() => (pipelines ?? []).filter((p) => p.active), [pipelines]);
  const [selectedPipelineId, setSelectedPipelineId] = useState<string | null>(null);

  const activePipeline = useMemo(() => {
    if (selectedPipelineId) {
      return activePipelines.find((p) => p.id === selectedPipelineId) ?? activePipelines[0] ?? null;
    }
    return activePipelines[0] ?? null;
  }, [activePipelines, selectedPipelineId]);

  const {
    data: opportunities = [],
    isLoading: opportunitiesLoading,
    error: opportunitiesError,
    refetch: refetchOpportunities,
  } = useOpportunities(companyId, activePipeline?.id ?? null);

  const createOpportunity = useCreateOpportunity(companyId, activePipeline?.id ?? null);
  const moveOpportunity = useMoveOpportunity(companyId);
  const markWon = useMarkWonOpportunity(companyId);
  const markLost = useMarkLostOpportunity(companyId);
  const deleteOpportunity = useDeleteOpportunity(companyId);

  const [createOpen, setCreateOpen] = useState(false);
  const [losing, setLosing] = useState<Opportunity | null>(null);
  const [toDelete, setToDelete] = useState<Opportunity | null>(null);

  const isLoading = pipelinesLoading || opportunitiesLoading;
  const error = pipelinesError ?? opportunitiesError;

  const handleRetry = () => {
    if (pipelinesError) refetchPipelines();
    if (opportunitiesError) refetchOpportunities();
  };

  const busyOpportunityId = moveOpportunity.isPending
    ? (moveOpportunity.variables?.id ?? null)
    : markWon.isPending
      ? (markWon.variables ?? null)
      : markLost.isPending
        ? (markLost.variables?.id ?? null)
        : deleteOpportunity.isPending
          ? (deleteOpportunity.variables ?? null)
          : null;

  if (!can("pipeline:page:view")) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center py-12 text-muted-foreground">
          <ShieldOff className="mb-4 h-10 w-10 opacity-50" />
          <p>Você não tem permissão para acessar a página de Negociações.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="space-y-1">
          <PageTitle>Negociações</PageTitle>
          <p className="text-sm text-muted-foreground">
            Acompanhe oportunidades e o pipeline comercial.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {activePipelines.length > 1 && (
            <Select
              value={activePipeline?.id ?? ""}
              onValueChange={(id) => setSelectedPipelineId(id)}
            >
              <SelectTrigger className="w-56">
                <SelectValue placeholder="Selecione o pipeline" />
              </SelectTrigger>
              <SelectContent>
                {activePipelines.map((pipeline) => (
                  <SelectItem key={pipeline.id} value={pipeline.id}>
                    {pipeline.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
          {canCreate && (
            <Button onClick={() => setCreateOpen(true)} disabled={!activePipeline}>
              <Plus className="mr-2 h-4 w-4" />
              Nova oportunidade
            </Button>
          )}
        </div>
      </div>

      {isLoading ? (
        <SkeletonTable rows={6} columns={4} />
      ) : error ? (
        <ErrorCard message={error.message} onRetry={handleRetry} />
      ) : activePipelines.length === 0 ? (
        <Card>
          <CardContent>
            <EmptyState
              icon={<Target className="h-8 w-8" />}
              title="Nenhum pipeline"
              description="Crie um pipeline para começar a vender."
            />
          </CardContent>
        </Card>
      ) : opportunities.length === 0 ? (
        <Card>
          <CardContent>
            <EmptyState
              icon={<Target className="h-8 w-8" />}
              title="Nenhuma oportunidade"
              description="Crie sua primeira oportunidade para começar a acompanhar o pipeline comercial."
              action={
                canCreate ? (
                  <Button variant="outline" size="sm" onClick={() => setCreateOpen(true)}>
                    Nova oportunidade
                  </Button>
                ) : undefined
              }
            />
          </CardContent>
        </Card>
      ) : (
        <PipelineBoard
          stages={activePipeline?.stages ?? []}
          opportunities={opportunities}
          canMove={canMove}
          canWin={canWin}
          canLose={canLose}
          canDelete={canDelete}
          busyOpportunityId={busyOpportunityId}
          onMove={(opportunity, direction) =>
            moveOpportunity.mutate({ id: opportunity.id, direction })
          }
          onWin={(opportunity) => markWon.mutate(opportunity.id)}
          onLost={(opportunity) => setLosing(opportunity)}
          onDelete={(opportunity) => setToDelete(opportunity)}
        />
      )}

      {activePipeline && (
        <CreateOpportunityDialog
          open={createOpen}
          onOpenChange={setCreateOpen}
          isLoading={createOpportunity.isPending}
          onSubmit={(values) =>
            createOpportunity.mutate(values, { onSuccess: () => setCreateOpen(false) })
          }
        />
      )}

      <LostReasonDialog
        open={!!losing}
        onOpenChange={(open) => !open && setLosing(null)}
        isLoading={markLost.isPending}
        onConfirm={(reason) => {
          if (losing) markLost.mutate({ id: losing.id, lossReason: reason });
          setLosing(null);
        }}
      />

      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        title="Excluir oportunidade"
        description={`Excluir a oportunidade "${toDelete?.title}"? Essa ação não pode ser desfeita.`}
        confirmLabel="Excluir"
        variant="destructive"
        isLoading={deleteOpportunity.isPending}
        onConfirm={() => {
          if (toDelete) deleteOpportunity.mutate(toDelete.id);
          setToDelete(null);
        }}
      />
    </div>
  );
}
