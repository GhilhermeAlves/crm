import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { CampaignStatus } from "../types/campaign.types";

const statusConfig: Record<CampaignStatus, { label: string; className: string }> = {
  DRAFT: {
    label: "Rascunho",
    className: "bg-muted text-muted-foreground",
  },
  SCHEDULED: {
    label: "Agendada",
    className: "bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300",
  },
  RUNNING: {
    label: "Em execução",
    className: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300",
  },
  PAUSED: {
    label: "Pausada",
    className: "bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-300",
  },
  COMPLETED: {
    label: "Concluída",
    className: "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300",
  },
  CANCELLED: {
    label: "Cancelada",
    className: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300",
  },
};

export function CampaignStatusBadge({ status }: { status: CampaignStatus }) {
  const config = statusConfig[status] ?? statusConfig.DRAFT;
  return (
    <Badge variant="secondary" className={cn(config.className)}>
      {config.label}
    </Badge>
  );
}
