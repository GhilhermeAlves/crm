"use client";

import type { Deal } from "../types/deal.types";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

type DeleteDealDialogProps = {
  deal: Deal | null;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
};

export function DeleteDealDialog({ deal, onOpenChange, onConfirm }: DeleteDealDialogProps) {
  return (
    <Dialog open={!!deal} onOpenChange={(o) => !o && onOpenChange(false)}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Excluir negociação</DialogTitle>
          <DialogDescription>
            Tem certeza que deseja excluir <strong>{deal?.name}</strong>? Esta ação não pode ser
            desfeita.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button variant="destructive" onClick={onConfirm}>
            Excluir
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
