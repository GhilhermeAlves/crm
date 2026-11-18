"use client";

import { useRouter } from "next/navigation";
import { Eye, Megaphone, MoreHorizontal, Pause, Pencil, Play, Trash2, XCircle } from "lucide-react";
import { AUDIENCE_SHORT_LABEL, isCampaignEditable, type Campaign } from "../types/campaign.types";
import { CampaignStatusBadge } from "./CampaignStatusBadge";
import { DataTable } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { campaignEditPath, ROUTES } from "@/lib/constants";

interface CampaignTableProps {
  campaigns: Campaign[];
  isLoading?: boolean;
  onDelete?: (campaign: Campaign) => void;
  onPause?: (campaign: Campaign) => void;
  onResume?: (campaign: Campaign) => void;
  onCancel?: (campaign: Campaign) => void;
  onExecute?: (campaign: Campaign) => void;
  /** Mostra "Editar" para campanhas em rascunho ou agendadas. */
  canEdit?: boolean;
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function CampaignTable({
  campaigns,
  isLoading,
  onDelete,
  onPause,
  onResume,
  onCancel,
  onExecute,
  canEdit = false,
}: CampaignTableProps) {
  const router = useRouter();

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-16 animate-pulse rounded bg-muted" />
        ))}
      </div>
    );
  }

  if (campaigns.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
        <Megaphone className="mb-4 h-12 w-12 opacity-50" />
        <p className="text-lg font-medium">Nenhuma campanha encontrada</p>
        <p className="text-sm">Crie uma campanha para começar a comunicar seus clientes.</p>
      </div>
    );
  }

  return (
    <DataTable
      tableId="campaigns"
      data={campaigns ?? []}
      columns={[
        {
          id: "nome",
          header: "Nome",
          cell: (campaign) => (
            <>
              <button
                type="button"
                onClick={() => router.push(`${ROUTES.CAMPAIGNS}/${campaign.id}`)}
                className="text-sm font-medium hover:underline"
              >
                {campaign.name}
              </button>
              {campaign.channelType && (
                <p className="text-xs text-muted-foreground">{campaign.channelType}</p>
              )}
            </>
          ),
        },
        {
          id: "status",
          header: "Status",
          cell: (campaign) => <CampaignStatusBadge status={campaign.status} />,
        },
        {
          id: "publico",
          header: "Público",
          className: "text-sm text-muted-foreground",
          cell: (campaign) => <>{AUDIENCE_SHORT_LABEL[campaign.audienceType]}</>,
        },
        {
          id: "destinatarios",
          header: "Destinatários",
          className: "text-sm font-medium",
          sortValue: (campaign) => campaign.estimatedRecipients,
          cell: (campaign) => <>{campaign.estimatedRecipients}</>,
        },
        {
          id: "agendamento",
          header: "Agendamento",
          className: "text-sm text-muted-foreground",
          sortValue: (campaign) => campaign.scheduledAt,
          cell: (campaign) => <>{formatDate(campaign.scheduledAt)}</>,
        },
        {
          id: "acoes",
          header: "",
          label: "Ações",
          hideable: false,
          headerClassName: "w-[60px]",
          cell: (campaign) => (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => router.push(`${ROUTES.CAMPAIGNS}/${campaign.id}`)}>
                  <Eye className="mr-2 h-4 w-4" />
                  Visualizar
                </DropdownMenuItem>
                {canEdit && isCampaignEditable(campaign.status) && (
                  <DropdownMenuItem onClick={() => router.push(campaignEditPath(campaign.id))}>
                    <Pencil className="mr-2 h-4 w-4" />
                    Editar
                  </DropdownMenuItem>
                )}
                {campaign.status === "RUNNING" && onPause && (
                  <DropdownMenuItem onClick={() => onPause(campaign)}>
                    <Pause className="mr-2 h-4 w-4" />
                    Pausar
                  </DropdownMenuItem>
                )}
                {campaign.status === "PAUSED" && onResume && (
                  <DropdownMenuItem onClick={() => onResume(campaign)}>
                    <Play className="mr-2 h-4 w-4" />
                    Retomar
                  </DropdownMenuItem>
                )}
                {(campaign.status === "DRAFT" || campaign.status === "SCHEDULED") && onExecute && (
                  <DropdownMenuItem onClick={() => onExecute(campaign)}>
                    <Play className="mr-2 h-4 w-4" />
                    Executar agora
                  </DropdownMenuItem>
                )}
                <DropdownMenuSeparator />
                {(campaign.status === "DRAFT" || campaign.status === "CANCELLED") && onDelete && (
                  <DropdownMenuItem onClick={() => onDelete(campaign)} className="text-destructive">
                    <Trash2 className="mr-2 h-4 w-4" />
                    Excluir
                  </DropdownMenuItem>
                )}
                {["SCHEDULED", "RUNNING", "PAUSED"].includes(campaign.status) && onCancel && (
                  <DropdownMenuItem onClick={() => onCancel(campaign)} className="text-destructive">
                    <XCircle className="mr-2 h-4 w-4" />
                    Cancelar campanha
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          ),
        },
      ]}
      keyExtractor={(campaign) => campaign.id}
    />
  );
}
