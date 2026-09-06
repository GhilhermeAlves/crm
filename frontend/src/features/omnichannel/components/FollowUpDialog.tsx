"use client";

import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import type { FollowUpRequest } from "../types/omnichannel.types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  conversationId: string;
  isSubmitting: boolean;
  onSubmit: (request: FollowUpRequest) => void;
};

/** Normaliza o valor do input datetime-local para ISO local (adiciona segundos). */
function normalizeLocalDateTime(value: string): string {
  return /:\d{2}$/.test(value) ? value : `${value}:00`;
}

export function FollowUpDialog({ open, onOpenChange, conversationId, isSubmitting, onSubmit }: Props) {
  const [executeAt, setExecuteAt] = useState("");
  const [content, setContent] = useState("");

  const reset = () => {
    setExecuteAt("");
    setContent("");
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const normalized = normalizeLocalDateTime(executeAt);
    if (!normalized || !content.trim()) {
      toast.error("Preencha a data/hora e o conteúdo do follow-up.");
      return;
    }
    onSubmit({ conversationId, content: content.trim(), executeAt: normalized });
    reset();
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) reset();
        onOpenChange(next);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Agendar follow-up</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="followup-datetime">Data e hora do envio</Label>
            <Input
              id="followup-datetime"
              type="datetime-local"
              value={executeAt}
              onChange={(e) => setExecuteAt(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="followup-content">Mensagem do follow-up</Label>
            <Textarea
              id="followup-content"
              placeholder="Ex.: Olá! Só passando pra saber se ficou alguma dúvida…"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={4}
              maxLength={1000}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Agendando…" : "Agendar"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}