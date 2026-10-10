"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { CreateAnamnesisModelRequest } from "../types/anamnesis.types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isSubmitting: boolean;
  onSubmit: (request: CreateAnamnesisModelRequest) => void;
};

export function AnamnesisModelDialog({ open, onOpenChange, isSubmitting, onSubmit }: Props) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  useEffect(() => {
    if (open) {
      setName("");
      setDescription("");
    }
  }, [open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      toast.error("Informe o nome do modelo.");
      return;
    }
    onSubmit({ name: name.trim(), description: description.trim() || undefined });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Novo modelo de anamnese</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="anamnesis-name">Nome do modelo</Label>
            <Input
              id="anamnesis-name"
              placeholder="Ex.: Anamnese de ortodontia"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={120}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="anamnesis-description">Descrição</Label>
            <Textarea
              id="anamnesis-description"
              placeholder="Opcional. Para que serve este modelo?"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              maxLength={4000}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Salvando…" : "Criar modelo"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
