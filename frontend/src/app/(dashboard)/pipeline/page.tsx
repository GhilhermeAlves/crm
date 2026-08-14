"use client";

import { useMemo, useState } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { usePipelines } from "@/features/pipeline/hooks/usePipelines";
import {
  useOpportunities,
  useCreateOpportunity,
  useMoveOpportunity,
  useMarkWonOpportunity,
  useMarkLostOpportunity,
  useDeleteOpportunity,
} from "@/features/pipeline/hooks/useOpportunities";
import { useQuery } from "@tanstack/react-query";
import { PipelineService } from "@/features/pipeline/services/pipeline.service";
import { PipelineBoard } from "@/features/pipeline/components/PipelineBoard";
import { PipelineMetricsStrip } from "@/features/pipeline/components/PipelineMetricsStrip";
import { CreateOpportunityDialog } from "@/features/pipeline/components/CreateOpportunityDialog";
import { LostReasonDialog } from "@/features/pipeline/components/LostReasonDialog";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type {
  Opportunity,
  MoveDirection,
} from "@/features/pipeline/types/pipeline.types";
import { useOpportunityPermissions } from "@/features/pipeline/schemas/pipeline.schema";

export default function PipelinePage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const [selectedPipelineId, setSelectedPipelineId] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [lostOpp, setLostOpp] = useState<Opportunity | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const perms = useOpportunityPermissions();

  const { data: pipelines = [], isLoading: pipelinesLoading } = usePipelines(companyId);
  const selectedPipelineIdResolved = selectedPipelineId ?? pipelines[0]?.id ?? null;

  const { data: opportunities = [] } = useOpportunities(
    companyId,
    selectedPipelineIdResolved
  );

  const activePipeline = useMemo(
    () => pipelines.find((p) => p.id === selectedPipelineIdResolved) ?? null,
    [pipelines, selectedPipelineIdResolved]
  );

  const { data: metrics } = useQuery({
    queryKey: ["pipeline-metrics", companyId, selectedPipelineIdResolved],
    queryFn: () =>
      PipelineService.metrics(companyId as string, selectedPipelineIdResolved as string),
    enabled: !!companyId && !!selectedPipelineIdResolved,
  });

  const createOpportunity = useCreateOpportunity(companyId, activePipeline?.id ?? null);
  const moveOpportunity = useMoveOpportunity(companyId);
  const markWon = useMarkWonOpportunity(companyId);
  const markLost = useMarkLostOpportunity(companyId);
  const deleteOpportunity = useDeleteOpportunity(companyId);

  const handleMove = (opportunity: Opportunity, direction: MoveDirection) => {
    setBusyId(opportunity.id);
    moveOpportunity.mutate(
      { id: opportunity.id, direction },
      { onSettled: () => setBusyId(null) }
    );
  };

  const handleWon = (opportunity: Opportunity) => {
    setBusyId(opportunity.id);
    markWon.mutate(opportunity.id, { onSettled: () => setBusyId(null) });
  };

  const handleDelete = (opportunity: Opportunity) => {
    setBusyId(opportunity.id);
    deleteOpportunity.mutate(opportunity.id, { onSettled: () => setBusyId(null) });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PageTitle>Pipeline</PageTitle>
        <div className="flex items-center gap-3">
          <Select
            value={selectedPipelineIdResolved ?? undefined}
            onValueChange={setSelectedPipelineId}
            disabled={pipelinesLoading}
          >
            <SelectTrigger className="w-56">
              <SelectValue placeholder="Selecione o pipeline" />
            </SelectTrigger>
            <SelectContent>
              {pipelines.map((pipeline) => (
                <SelectItem key={pipeline.id} value={pipeline.id}>
                  {pipeline.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {perms.canCreate && (
            <Button onClick={() => setCreateOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Nova Oportunidade
            </Button>
          )}
        </div>
      </div>

      <PipelineMetricsStrip metrics={metrics} isLoading={!metrics} />

      <PipelineBoard
        stages={activePipeline?.stages ?? []}
        opportunities={opportunities}
        canMove={perms.canMove}
        canWin={perms.canWin}
        canLose={perms.canLose}
        canDelete={perms.canDelete}
        busyOpportunityId={busyId}
        onMove={handleMove}
        onWin={handleWon}
        onLost={setLostOpp}
        onDelete={handleDelete}
      />

      <CreateOpportunityDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        isLoading={createOpportunity.isPending}
        onSubmit={(values) =>
          createOpportunity.mutate(values, { onSuccess: () => setCreateOpen(false) })
        }
      />

      <LostReasonDialog
        open={!!lostOpp}
        onOpenChange={(open) => !open && setLostOpp(null)}
        isLoading={markLost.isPending}
        onConfirm={(reason) => {
          if (lostOpp) {
            markLost.mutate(
              { id: lostOpp.id, lossReason: reason },
              { onSuccess: () => setLostOpp(null) }
            );
          }
        }}
      />
    </div>
  );
}
