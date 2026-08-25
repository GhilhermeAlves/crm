"use client";

import type { Campaign } from "../types/campaign.types";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface ConfirmCampaignDialogProps {
  campaign: Campaign | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  isLoading?: boolean;
}

export function DeleteCampaignDialog({
  campaign,
  open,
  onOpenChange,
  onConfirm,
  isLoading,
}: ConfirmCampaignDialogProps) {
  if (!campaign) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Excluir Campanha</DialogTitle>
          <DialogDescription>
            Tem certeza que deseja excluir a campanha &quot;{campaign.name}&quot;? Esta ação não
            pode ser desfeita.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button variant="destructive" onClick={onConfirm} disabled={isLoading}>
            {isLoading ? "Excluindo..." : "Excluir"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function CancelCampaignDialog({
  campaign,
  open,
  onOpenChange,
  onConfirm,
  isLoading,
}: ConfirmCampaignDialogProps) {
  if (!campaign) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Cancelar Campanha</DialogTitle>
          <DialogDescription>
            Cancelar a campanha &quot;{campaign.name}&quot; interrompe os envios pendentes.
            Mensagens já enviadas não são afetadas. Deseja continuar?
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Voltar
          </Button>
          <Button variant="destructive" onClick={onConfirm} disabled={isLoading}>
            {isLoading ? "Cancelando..." : "Cancelar campanha"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
