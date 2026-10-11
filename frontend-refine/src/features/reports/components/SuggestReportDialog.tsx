"use client";

import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";

import { useSubmitReportSuggestion } from "../hooks/useReports";
import {
  REPORT_SUGGESTION_CATEGORIES,
  REPORT_SUGGESTION_CATEGORY_LABELS,
  type ReportSuggestionCategory,
} from "../types/reports.types";

type SuggestReportDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string | null;
};

export function SuggestReportDialog({ open, onOpenChange, companyId }: SuggestReportDialogProps) {
  const [step, setStep] = useState<1 | 2>(1);
  const [selected, setSelected] = useState<ReportSuggestionCategory[]>([]);
  const [notes, setNotes] = useState("");

  const submit = useSubmitReportSuggestion(companyId);

  // Ao fechar, o fluxo volta ao estado inicial.
  useEffect(() => {
    if (open) return;
    setStep(1);
    setSelected([]);
    setNotes("");
  }, [open]);

  const toggle = (category: ReportSuggestionCategory) => {
    setSelected((current) =>
      current.includes(category)
        ? current.filter((item) => item !== category)
        : [...current, category],
    );
  };

  const handleSubmit = () => {
    if (selected.length === 0) return;
    submit.mutate(
      { categories: selected, notes: notes.trim() || undefined },
      {
        onSuccess: () => {
          onOpenChange(false);
          toast.success("Recebemos sua sugestão!");
        },
        onError: (error: Error) => {
          toast.error(error.message || "Erro ao enviar a sugestão.");
        },
      },
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[440px]">
        {step === 1 ? (
          <>
            <DialogHeader>
              <DialogTitle>Compartilhe sua sugestão de relatório</DialogTitle>
              <DialogDescription>
                Escolha os relatórios mais relevantes para o seu dia a dia.
              </DialogDescription>
            </DialogHeader>

            <div className="grid grid-cols-2 gap-3">
              {REPORT_SUGGESTION_CATEGORIES.map((category) => {
                const checked = selected.includes(category);
                return (
                  <label
                    key={category}
                    className={cn(
                      "flex cursor-pointer items-center gap-2 rounded-lg border p-3 text-sm transition-colors",
                      checked ? "border-primary bg-primary/5" : "border-input hover:bg-muted/40",
                    )}
                  >
                    <Checkbox
                      checked={checked}
                      onCheckedChange={() => toggle(category)}
                      aria-label={REPORT_SUGGESTION_CATEGORY_LABELS[category]}
                    />
                    <span className="font-medium">
                      {REPORT_SUGGESTION_CATEGORY_LABELS[category]}
                    </span>
                  </label>
                );
              })}
            </div>

            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="button" disabled={selected.length === 0} onClick={() => setStep(2)}>
                Continuar
              </Button>
            </div>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Mais contexto para sua escolha</DialogTitle>
            </DialogHeader>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="report-suggestion-notes">Deseja incluir alguma observação?</Label>
                <span className="text-xs text-muted-foreground">Opcional</span>
              </div>
              <Textarea
                id="report-suggestion-notes"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Descreva o que você espera ver neste relatório"
                rows={4}
              />
            </div>

            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="button" disabled={submit.isPending} onClick={handleSubmit}>
                {submit.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Enviando…
                  </>
                ) : (
                  "Enviar"
                )}
              </Button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
