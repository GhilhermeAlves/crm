"use client";

import { useId, useState } from "react";
import { format } from "date-fns";
import { ClipboardPlus, Loader2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAssessments, useOpenAssessment, useTrichologySettings } from "../hooks/useTrichology";
import { STATUS_LABELS } from "../types/trichology.types";

type Props = {
  companyId: string;
  contactId: string;
  canWrite: boolean;
  onOpen: (id: string) => void;
};

/** Exames tricológicos do paciente e abertura de um novo (com consentimento LGPD). */
export function AssessmentList({ companyId, contactId, canWrite, onOpen }: Props) {
  const uid = useId();
  const { data: assessments, isLoading } = useAssessments(companyId, contactId);
  const settings = useTrichologySettings(companyId);
  const open = useOpenAssessment(companyId, contactId);
  const [dialog, setDialog] = useState(false);
  const [consent, setConsent] = useState(false);
  const [complaint, setComplaint] = useState("");
  const [protocolId, setProtocolId] = useState<string>("");

  const protocols = (settings.data?.protocols ?? []).filter((p) => p.active);
  const defaultProtocol = protocols.find((p) => p.isDefault) ?? protocols[0];

  const submit = () =>
    open.mutate(
      {
        consentGiven: consent,
        chiefComplaint: complaint || undefined,
        protocolId: protocolId || defaultProtocol?.id,
      },
      {
        onSuccess: (created) => {
          setDialog(false);
          setConsent(false);
          setComplaint("");
          onOpen(created.id);
        },
      },
    );

  return (
    <section className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-lg font-semibold">Exames tricológicos</h2>
        {canWrite && (
          <Button type="button" onClick={() => setDialog(true)}>
            <ClipboardPlus className="mr-2 h-4 w-4" /> Novo exame
          </Button>
        )}
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Carregando…</p>
      ) : !assessments || assessments.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhum exame ainda.</p>
      ) : (
        <ul className="divide-y rounded-md border">
          {assessments.map((a) => (
            <li key={a.id}>
              <button
                type="button"
                className="flex w-full flex-wrap items-center gap-2 p-3 text-left hover:bg-muted/50"
                onClick={() => onOpen(a.id)}
              >
                <span className="font-medium">{format(new Date(a.createdAt), "dd/MM/yyyy")}</span>
                {a.reportVersion > 1 && (
                  <span className="text-xs text-muted-foreground">versão {a.reportVersion}</span>
                )}
                <Badge variant={a.status === "PUBLISHED" ? "default" : "secondary"}>
                  {STATUS_LABELS[a.status]}
                </Badge>
                {!a.trichoscopyPerformed && <Badge variant="outline">Sem tricoscopia</Badge>}
                {a.professionalName && (
                  <span className="ml-auto text-xs text-muted-foreground">
                    {a.professionalName}
                  </span>
                )}
              </button>
            </li>
          ))}
        </ul>
      )}

      <Dialog open={dialog} onOpenChange={setDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Novo exame tricológico</DialogTitle>
            <DialogDescription>
              O exame reúne quiz, fotos por região, exame clínico, laboratório e relatório.
            </DialogDescription>
          </DialogHeader>
          {protocols.length > 1 && (
            <div className="space-y-1">
              <Label htmlFor={`${uid}-protocol`}>Protocolo de captura</Label>
              <Select value={protocolId || defaultProtocol?.id || ""} onValueChange={setProtocolId}>
                <SelectTrigger id={`${uid}-protocol`}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {protocols.map((p) => (
                    <SelectItem key={p.id} value={p.id}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
          <div className="space-y-1">
            <Label htmlFor={`${uid}-complaint`}>Queixa principal (opcional)</Label>
            <Textarea
              id={`${uid}-complaint`}
              value={complaint}
              maxLength={2000}
              onChange={(e) => setComplaint(e.target.value)}
            />
          </div>
          <div className="flex items-start gap-2">
            <Checkbox
              id={`${uid}-consent`}
              checked={consent}
              onCheckedChange={(c) => setConsent(Boolean(c))}
            />
            <Label htmlFor={`${uid}-consent`} className="font-normal leading-snug">
              O paciente autorizou o uso das imagens e dados de saúde neste exame, inclusive a
              análise por inteligência artificial sem dados que o identifiquem (LGPD, art. 11).
            </Label>
          </div>
          <DialogFooter>
            <Button type="button" onClick={submit} disabled={!consent || open.isPending}>
              {open.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Abrir exame
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  );
}
