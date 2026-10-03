"use client";

import type { Lead } from "../types/lead.types";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface ConvertLeadDialogProps {
  lead: Lead | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  isLoading?: boolean;
}

export function ConvertLeadDialog({
  lead,
  open,
  onOpenChange,
  onConfirm,
  isLoading,
}: ConvertLeadDialogProps) {
  if (!lead) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Converter Lead em Contato</DialogTitle>
          <DialogDescription>
            Este lead já está associado a um contato. Ao confirmar, o lead será marcado como{" "}
            <strong>Convertido</strong>, encerrando o funil de qualificação e consolidando o contato
            relacionado.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={onConfirm} disabled={isLoading}>
            {isLoading ? "Convertendo..." : "Converter em contato"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
