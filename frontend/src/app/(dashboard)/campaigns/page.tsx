"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import {
  useCampaigns,
  useDeleteCampaign,
  usePauseCampaign,
  useResumeCampaign,
  useCancelCampaign,
  useExecuteCampaign,
} from "@/features/campaigns/hooks/useCampaigns";
import { CampaignTable } from "@/features/campaigns/components/CampaignTable";
import {
  DeleteCampaignDialog,
  CancelCampaignDialog,
} from "@/features/campaigns/components/CampaignDialogs";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Plus, RefreshCcw } from "lucide-react";
import type {
  AudienceType,
  Campaign,
  CampaignStatus,
} from "@/features/campaigns/types/campaign.types";
import { ROUTES } from "@/lib/constants";

export default function CampaignsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const [status, setStatus] = useState("all");
  const [audienceType, setAudienceType] = useState("all");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [deleteTarget, setDeleteTarget] = useState<Campaign | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Campaign | null>(null);

  const { data, isLoading, refetch } = useCampaigns(companyId, {
    page,
    pageSize: 10,
    status: status !== "all" ? (status as CampaignStatus) : undefined,
    audienceType: audienceType !== "all" ? (audienceType as AudienceType) : undefined,
  });

  const deleteMutation = useDeleteCampaign(companyId);
  const pauseMutation = usePauseCampaign(companyId);
  const resumeMutation = useResumeCampaign(companyId);
  const cancelMutation = useCancelCampaign(companyId);
  const executeMutation = useExecuteCampaign(companyId);

  const canCreate = can("campaign:create");
  const canExecute = can("campaign:execute");
  const canDelete = can("campaign:delete");

  const campaigns = (data?.content || []).filter((c) =>
    search ? c.name.toLowerCase().includes(search.toLowerCase()) : true,
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageTitle>Campanhas</PageTitle>
        {canCreate && (
          <Button onClick={() => router.push(ROUTES.CAMPAIGNS_NEW)}>
            <Plus className="mr-2 h-4 w-4" />
            Nova Campanha
          </Button>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Input
          placeholder="Buscar por nome..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="max-w-xs"
          aria-label="Buscar campanhas"
        />
        <Select
          value={status}
          onValueChange={(v) => {
            setStatus(v);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-[180px]" aria-label="Filtrar por status">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos os status</SelectItem>
            <SelectItem value="DRAFT">Rascunho</SelectItem>
            <SelectItem value="SCHEDULED">Agendada</SelectItem>
            <SelectItem value="RUNNING">Em execução</SelectItem>
            <SelectItem value="PAUSED">Pausada</SelectItem>
            <SelectItem value="COMPLETED">Concluída</SelectItem>
            <SelectItem value="CANCELLED">Cancelada</SelectItem>
          </SelectContent>
        </Select>
        <Select
          value={audienceType}
          onValueChange={(v) => {
            setAudienceType(v);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-[170px]" aria-label="Filtrar por público">
            <SelectValue placeholder="Público" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todo público</SelectItem>
            <SelectItem value="CONTACTS">Contatos</SelectItem>
            <SelectItem value="LEADS">Leads</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" size="icon" onClick={() => refetch()} aria-label="Atualizar">
          <RefreshCcw className="h-4 w-4" />
        </Button>
      </div>

      <CampaignTable
        campaigns={campaigns}
        isLoading={isLoading}
        onDelete={canDelete ? setDeleteTarget : undefined}
        onPause={canExecute ? (c) => pauseMutation.mutate(c.id) : undefined}
        onResume={canExecute ? (c) => resumeMutation.mutate(c.id) : undefined}
        onCancel={canExecute ? setCancelTarget : undefined}
        onExecute={
          canExecute
            ? (c) => {
                if (
                  window.confirm(
                    `Executar a campanha "${c.name}" agora para ${c.estimatedRecipients} destinatários?`,
                  )
                ) {
                  executeMutation.mutate(c.id);
                }
              }
            : undefined
        }
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Mostrando {data.content.length} de {data.totalElements} campanhas
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
              disabled={page >= data.totalPages - 1}
            >
              Próximo
            </Button>
          </div>
        </div>
      )}

      <DeleteCampaignDialog
        campaign={deleteTarget}
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) {
            deleteMutation.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) });
          }
        }}
        isLoading={deleteMutation.isPending}
      />

      <CancelCampaignDialog
        campaign={cancelTarget}
        open={!!cancelTarget}
        onOpenChange={(open) => !open && setCancelTarget(null)}
        onConfirm={() => {
          if (cancelTarget) {
            cancelMutation.mutate(cancelTarget.id, { onSuccess: () => setCancelTarget(null) });
          }
        }}
        isLoading={cancelMutation.isPending}
      />
    </div>
  );
}
