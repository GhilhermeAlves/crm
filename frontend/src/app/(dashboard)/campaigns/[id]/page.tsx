"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import {
  useCampaign,
  useCampaignExecution,
  usePauseCampaign,
  useResumeCampaign,
  useCancelCampaign,
  useExecuteCampaign,
} from "@/features/campaigns/hooks/useCampaigns";
import { CampaignStatusBadge } from "@/features/campaigns/components/CampaignStatusBadge";
import { CancelCampaignDialog } from "@/features/campaigns/components/CampaignDialogs";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

function formatDateTime(dateStr: string | null): string {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleString("pt-BR");
}

export default function CampaignDetailPage({ params }: { params: { id: string } }) {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;
  const campaignId = params.id;

  const [cancelOpen, setCancelOpen] = useState(false);

  const { data: campaign, isLoading, isError } = useCampaign(companyId, campaignId);
  const isLive = campaign?.status === "RUNNING" || campaign?.status === "PAUSED";
  const { data: execution } = useCampaignExecution(companyId, campaignId, {
    refetchInterval: isLive ? 5000 : undefined,
  });

  const pauseMutation = usePauseCampaign(companyId);
  const resumeMutation = useResumeCampaign(companyId);
  const cancelMutation = useCancelCampaign(companyId);
  const executeMutation = useExecuteCampaign(companyId);

  const canExecute = can("campaign:execute");

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-10 w-64 animate-pulse rounded bg-muted" />
        <div className="h-48 animate-pulse rounded bg-muted" />
      </div>
    );
  }

  if (isError || !campaign) {
    return (
      <div className="py-12 text-center text-muted-foreground">
        Campanha não encontrada ou sem acesso.
      </div>
    );
  }

  const progress =
    execution && execution.totalRecipients > 0
      ? Math.round((execution.processedCount / execution.totalRecipients) * 100)
      : 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">{campaign.name}</h1>
          <CampaignStatusBadge status={campaign.status} />
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link href="/campaigns">Voltar</Link>
          </Button>
          {canExecute && campaign.status === "RUNNING" && (
            <Button variant="outline" onClick={() => pauseMutation.mutate(campaign.id)}>
              Pausar
            </Button>
          )}
          {canExecute && campaign.status === "PAUSED" && (
            <Button variant="outline" onClick={() => resumeMutation.mutate(campaign.id)}>
              Retomar
            </Button>
          )}
          {canExecute && ["DRAFT", "SCHEDULED"].includes(campaign.status) && (
            <Button onClick={() => executeMutation.mutate(campaign.id)}>Executar agora</Button>
          )}
          {canExecute && ["SCHEDULED", "RUNNING", "PAUSED"].includes(campaign.status) && (
            <Button variant="destructive" onClick={() => setCancelOpen(true)}>
              Cancelar
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Informações</CardTitle>
          </CardHeader>
          <CardContent className="space-y-1 text-sm">
            <p>
              <span className="font-medium">Descrição:</span> {campaign.description ?? "—"}
            </p>
            <p>
              <span className="font-medium">Público:</span>{" "}
              {campaign.audienceType === "CONTACTS" ? "Contatos" : "Leads"}
            </p>
            <p>
              <span className="font-medium">Destinatários estimados:</span>{" "}
              {campaign.estimatedRecipients}
            </p>
            <p>
              <span className="font-medium">Canal:</span>{" "}
              {campaign.channelType ?? "não configurado"}
            </p>
            <p>
              <span className="font-medium">Agendada para:</span>{" "}
              {formatDateTime(campaign.scheduledAt)}
            </p>
            <p>
              <span className="font-medium">Início:</span> {formatDateTime(campaign.startedAt)}
            </p>
            <p>
              <span className="font-medium">Conclusão:</span> {formatDateTime(campaign.completedAt)}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Execução</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {!execution ? (
              <p className="text-muted-foreground">Campanha ainda não foi executada.</p>
            ) : (
              <>
                <div
                  className="h-2 w-full overflow-hidden rounded-full bg-muted"
                  role="progressbar"
                  aria-valuenow={progress}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label="Progresso da execução"
                >
                  <div
                    className="h-full rounded-full bg-primary transition-all"
                    style={{ width: `${progress}%` }}
                  />
                </div>
                <p>
                  {execution.processedCount} de {execution.totalRecipients} processados ({progress}
                  %)
                </p>
                <p>
                  <span className="font-medium">Falhas:</span> {execution.failedCount}
                </p>
                <p>
                  <span className="font-medium">Início:</span> {formatDateTime(execution.startedAt)}
                </p>
                <p>
                  <span className="font-medium">Fim:</span> {formatDateTime(execution.finishedAt)}
                </p>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {execution && (
        <Card>
          <CardHeader>
            <CardTitle>Resumo</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Total</TableHead>
                  <TableHead>Processados</TableHead>
                  <TableHead>Falhas</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow>
                  <TableCell>{execution.totalRecipients}</TableCell>
                  <TableCell>{execution.processedCount}</TableCell>
                  <TableCell>{execution.failedCount}</TableCell>
                  <TableCell>{execution.status}</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      <CancelCampaignDialog
        campaign={campaign}
        open={cancelOpen}
        onOpenChange={setCancelOpen}
        onConfirm={() =>
          cancelMutation.mutate(campaign.id, { onSuccess: () => setCancelOpen(false) })
        }
        isLoading={cancelMutation.isPending}
      />
    </div>
  );
}
