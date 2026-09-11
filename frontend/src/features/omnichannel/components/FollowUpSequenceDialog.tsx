"use client";

import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import type { FollowUpSequenceRequest } from "../types/followup-sequence.types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialName?: string;
  initialDescription?: string;
  isSubmitting: boolean;
  onSubmit: (request: FollowUpSequenceRequest) => void;
};

export function FollowUpSequenceDialog({
  open,
  onOpenChange,
  initialName,
  initialDescription,
  isSubmitting,
  onSubmit,
}: Props) {
  const [name, setName] = useState(initialName ?? "");
  const [description, setDescription] = useState(initialDescription ?? "");

  useEffect(() => {
    if (open) {
      setName(initialName ?? "");
      setDescription(initialDescription ?? "");
    }
  }, [open, initialName, initialDescription]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      toast.error("Preencha o nome da sequência.");
      return;
    }
    onSubmit({
      name: name.trim(),
      description: description.trim() || undefined,
    });
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          setName("");
          setDescription("");
        }
        onOpenChange(next);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {initialName ? "Editar sequência" : "Nova sequência de follow-up"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="seq-name">Nome</Label>
            <Input
              id="seq-name"
              placeholder="Ex.: Carrinho abandonado"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={120}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="seq-desc">Descrição</Label>
            <Textarea
              id="seq-desc"
              placeholder="Descreva o propósito desta sequência de follow-up…"
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
              {isSubmitting ? "Salvando…" : "Salvar"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
