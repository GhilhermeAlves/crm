"use client";

import { useState } from "react";
import { format } from "date-fns";
import { ArrowLeft, Ban } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useAssessment, useAssessmentMutations, useCopilot } from "../hooks/useTrichology";
import { STATUS_LABELS } from "../types/trichology.types";
import Link from "next/link";
import { CaptureStep, ClinicalStep, LabsStep } from "./AssessmentSteps";
import { CopilotPanel } from "./CopilotPanel";
import { ReportStep } from "./ReportStep";

type Props = {
  companyId: string;
  contactId: string;
  assessmentId: string;
  onBack: () => void;
  onOpenAssessment: (id: string) => void;
};

/** Tela do exame em etapas, com o copiloto ao lado (doc v1.2, seção 9). */
export function AssessmentWorkspace({
  companyId,
  contactId,
  assessmentId,
  onBack,
  onOpenAssessment,
}: Props) {
  const { can } = useAuthorization();
  const canWrite = can("trichology:write");
  const canApprove = can("trichology:approve");
  const { data: assessment, isLoading } = useAssessment(companyId, contactId, assessmentId);
  const mutations = useAssessmentMutations(companyId, contactId, assessmentId);
  const copilot = useCopilot(companyId, contactId, assessmentId, Boolean(assessment));
  const [cancelling, setCancelling] = useState(false);
  const [reason, setReason] = useState("");

  if (isLoading || !assessment) {
    return <p className="text-sm text-muted-foreground">Carregando exame…</p>;
  }
  const finished = assessment.status === "PUBLISHED" || assessment.status === "CANCELLED";
  const editable = canWrite && !finished;
  const step = { companyId, contactId, assessment, editable, mutations };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <Button type="button" variant="ghost" size="sm" onClick={onBack}>
          <ArrowLeft className="mr-2 h-4 w-4" /> Exames
        </Button>
        <h2 className="text-lg font-semibold">
          Exame de {format(new Date(assessment.createdAt), "dd/MM/yyyy")}
          {assessment.reportVersion > 1 ? ` · versão ${assessment.reportVersion}` : ""}
        </h2>
        <Badge variant={assessment.status === "PUBLISHED" ? "default" : "secondary"}>
          {STATUS_LABELS[assessment.status]}
        </Badge>
        {editable && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="ml-auto text-destructive"
            onClick={() => setCancelling(true)}
          >
            <Ban className="mr-2 h-4 w-4" /> Cancelar exame
          </Button>
        )}
      </div>
      {assessment.chiefComplaint && (
        <p className="text-sm">
          <span className="text-muted-foreground">Queixa: </span>
          {assessment.chiefComplaint}
        </p>
      )}
      <p className="text-sm">
        <span className="text-muted-foreground">Anamnese: </span>
        {assessment.anamnesisCompletedAt ? (
          <>
            concluída em {format(new Date(assessment.anamnesisCompletedAt), "dd/MM/yyyy")} ·{" "}
            <Link className="underline" href={`/patients/${contactId}/anamnese`}>
              ver respostas
            </Link>
          </>
        ) : (
          <>
            sem anamnese concluída ·{" "}
            <Link className="underline" href={`/patients/${contactId}/anamnese`}>
              enviar anamnese
            </Link>
          </>
        )}
      </p>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <Tabs defaultValue={assessment.report ? "report" : "capture"}>
          <TabsList className="flex w-full flex-wrap justify-start">
            <TabsTrigger value="capture">Captura</TabsTrigger>
            <TabsTrigger value="clinical">Clínico</TabsTrigger>
            <TabsTrigger value="labs">Laboratório</TabsTrigger>
            <TabsTrigger value="report">Relatório</TabsTrigger>
          </TabsList>
          <TabsContent value="capture" className="pt-4">
            <CaptureStep {...step} />
          </TabsContent>
          <TabsContent value="clinical" className="pt-4">
            <ClinicalStep {...step} />
          </TabsContent>
          <TabsContent value="labs" className="pt-4">
            <LabsStep {...step} copilot={copilot.data} />
          </TabsContent>
          <TabsContent value="report" className="pt-4">
            <ReportStep
              companyId={companyId}
              contactId={contactId}
              assessment={assessment}
              canWrite={canWrite}
              canApprove={canApprove}
              mutations={mutations}
              onOpenAssessment={onOpenAssessment}
            />
          </TabsContent>
        </Tabs>
        <aside>
          <CopilotPanel copilot={copilot.data} loading={copilot.isLoading} />
        </aside>
      </div>

      <Dialog open={cancelling} onOpenChange={setCancelling}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancelar exame</DialogTitle>
            <DialogDescription>As fotos continuam na galeria do paciente.</DialogDescription>
          </DialogHeader>
          <Input
            value={reason}
            maxLength={500}
            placeholder="Motivo (opcional)"
            aria-label="Motivo do cancelamento"
            onChange={(e) => setReason(e.target.value)}
          />
          <DialogFooter>
            <Button
              type="button"
              variant="destructive"
              disabled={mutations.cancel.isPending}
              onClick={() =>
                mutations.cancel.mutate(reason || undefined, {
                  onSuccess: () => setCancelling(false),
                })
              }
            >
              Cancelar exame
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
