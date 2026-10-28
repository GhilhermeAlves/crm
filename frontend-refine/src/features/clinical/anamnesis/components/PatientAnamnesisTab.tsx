"use client";

import { useState } from "react";
import { format, parseISO } from "date-fns";
import { Clock, PenLine, Send, Tablet } from "lucide-react";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { useAnamnesisModels } from "@/features/masterdata/anamnesis/hooks/useAnamnesis";
import {
  useCurrentPatientAnamnesis,
  useDiscardPatientAnamnesis,
  usePatientAnamneses,
  useStartPatientAnamnesis,
} from "../hooks/usePatientAnamnesis";
import { type FormRequest, usePatientFormRequests } from "../hooks/usePatientFormRequests";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { AnamnesisAnswerForm } from "./AnamnesisAnswerForm";
import { AnamnesisAnswerView } from "./AnamnesisAnswerView";
import { SendFormDialog } from "./SendFormDialog";
import type { PatientAnamnesisResponse } from "../types/patient-anamnesis.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const formatDateTime = (value: string) => format(parseISO(value), "dd/MM/yyyy 'às' HH:mm");

function PatientAnswerBadge({ request }: { request?: FormRequest }) {
  if (!request?.answeredAt) return null;
  return (
    <Badge variant="secondary" className="gap-1">
      <PenLine className="h-3 w-3" /> Respondido e assinado pelo paciente em{" "}
      {formatDateTime(request.answeredAt)}
    </Badge>
  );
}

function Signature({ request }: { request?: FormRequest }) {
  if (!request?.signatureUrl) return null;
  return (
    <div className="space-y-1">
      <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
        Assinatura do paciente
      </p>
      {/* eslint-disable-next-line @next/next/no-img-element -- imagem autenticada pelo cookie */}
      <img
        src={request.signatureUrl}
        alt="Assinatura do paciente"
        className="h-24 rounded border bg-white object-contain p-1"
      />
    </div>
  );
}

export function PatientAnamnesisTab({
  companyId,
  contactId,
  patient,
}: {
  companyId: string;
  contactId: string;
  patient: Contact;
}) {
  const can = usePermission();
  const canUpdate = can("anamnesis:manage") || can("contact:update");
  const { data: models = [] } = useAnamnesisModels(companyId);
  const { data: current, isLoading: loadingCurrent } = useCurrentPatientAnamnesis(
    companyId,
    contactId,
  );
  const { data: history = [] } = usePatientAnamneses(companyId, contactId);
  const { data: requests = [] } = usePatientFormRequests(companyId, contactId);
  const startMutation = useStartPatientAnamnesis(companyId, contactId);
  const discardMutation = useDiscardPatientAnamnesis(companyId, contactId);
  const [selectedModel, setSelectedModel] = useState<string | undefined>(undefined);
  const [viewing, setViewing] = useState<string | null>(null);
  const [sendOpen, setSendOpen] = useState(false);

  const activeModels = models.filter((m) => m.active);
  const defaultModel = activeModels.find((m) => m.isDefault) || activeModels[0];
  const effectiveModel = selectedModel || defaultModel?.id;

  const requestByAnamnesis = new Map(requests.map((r) => [r.anamnesisId, r]));
  const drafts = history.filter((h) => h.status === "DRAFT");
  // Rascunho de pedido ainda não respondido: é do paciente, não se preenche aqui.
  const waiting = drafts
    .filter((d) => requestByAnamnesis.get(d.id)?.status !== "ANSWERED")
    .filter((d) => requestByAnamnesis.has(d.id));
  const editable = drafts.filter((d) => !waiting.includes(d));
  const completedHistory = history.filter((h) => h.status === "COMPLETED" && h.id !== current?.id);
  const viewingItem = viewing ? history.find((h) => h.id === viewing) : undefined;

  if (loadingCurrent) {
    return <p className="text-sm text-muted-foreground">Carregando...</p>;
  }

  const discardButton = (draft: PatientAnamnesisResponse, label: string) => (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="outline" size="sm">
          {label}
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{label}?</AlertDialogTitle>
          <AlertDialogDescription>
            O rascunho é apagado e, se foi enviado ao paciente, o link deixa de funcionar.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Voltar</AlertDialogCancel>
          <AlertDialogAction onClick={() => discardMutation.mutate(draft.id)}>
            {label}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );

  return (
    <div className="space-y-6">
      {canUpdate && activeModels.length > 0 && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <Select value={effectiveModel} onValueChange={setSelectedModel}>
            <SelectTrigger className="w-full sm:w-[320px]">
              <SelectValue placeholder="Selecione um modelo" />
            </SelectTrigger>
            <SelectContent>
              {activeModels.map((model) => (
                <SelectItem key={model.id} value={model.id}>
                  {model.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            variant="outline"
            disabled={!effectiveModel || startMutation.isPending}
            onClick={() => effectiveModel && startMutation.mutate({ modelId: effectiveModel })}
          >
            Preencher agora
          </Button>
          <Button className="gap-1.5" onClick={() => setSendOpen(true)}>
            <Send className="h-4 w-4" /> Enviar ao paciente
          </Button>
        </div>
      )}
      {canUpdate && activeModels.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Nenhum modelo de anamnese ativo. Crie ou importe um em Configurações → Documentos.
        </p>
      )}

      {waiting.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Aguardando o paciente</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {waiting.map((draft) => {
              const request = requestByAnamnesis.get(draft.id)!;
              const expired = request.status === "EXPIRED";
              return (
                <div
                  key={draft.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-md border p-3"
                >
                  <div className="min-w-0">
                    <p className="flex items-center gap-1.5 text-sm font-medium">
                      {request.mode === "RECEPTION" ? (
                        <Tablet className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <Send className="h-4 w-4 text-muted-foreground" />
                      )}
                      {draft.modelName}
                    </p>
                    <p className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Clock className="h-3 w-3" />
                      {expired
                        ? `Link expirado em ${formatDateTime(request.expiresAt)}`
                        : `Enviado em ${formatDateTime(request.createdAt)} · vale até ${formatDateTime(request.expiresAt)}`}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {expired && <Badge variant="destructive">Expirado</Badge>}
                    {canUpdate && discardButton(draft, "Cancelar envio")}
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      )}

      {editable.map((draft) => {
        const request = requestByAnamnesis.get(draft.id);
        return (
          <Card key={draft.id}>
            <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-2">
              <div className="space-y-1">
                <CardTitle className="text-base">
                  {request
                    ? "Respondida pelo paciente — complete e conclua"
                    : "Anamnese em rascunho"}
                  : {draft.modelName}
                </CardTitle>
                <PatientAnswerBadge request={request} />
              </div>
              {canUpdate && discardButton(draft, "Descartar")}
            </CardHeader>
            <CardContent className="space-y-4">
              <Signature request={request} />
              {canUpdate ? (
                <AnamnesisAnswerForm
                  companyId={companyId}
                  contactId={contactId}
                  anamnesis={draft}
                />
              ) : (
                <AnamnesisAnswerView anamnesis={draft} />
              )}
            </CardContent>
          </Card>
        );
      })}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Anamnese atual</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {current ? (
            <>
              <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                <span>
                  {current.modelName} · concluída
                  {current.completedByName ? ` por ${current.completedByName}` : ""} em{" "}
                  {current.completedAt ? formatDateTime(current.completedAt) : "—"}
                </span>
                <PatientAnswerBadge request={requestByAnamnesis.get(current.id)} />
              </div>
              <AnamnesisAnswerView anamnesis={current} />
              <Signature request={requestByAnamnesis.get(current.id)} />
            </>
          ) : (
            <p className="text-sm text-muted-foreground">Nenhuma anamnese concluída.</p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Histórico</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {completedHistory.length === 0 ? (
            <p className="text-sm text-muted-foreground">Sem histórico.</p>
          ) : (
            completedHistory.map((item) => (
              <div
                key={item.id}
                className="flex items-center justify-between rounded-md border p-3"
              >
                <div>
                  <p className="text-sm font-medium">{item.modelName}</p>
                  <p className="text-xs text-muted-foreground">
                    {item.completedAt ? formatDateTime(item.completedAt) : "—"}
                    {requestByAnamnesis.get(item.id)?.answeredAt
                      ? " · respondida pelo paciente"
                      : ""}
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setViewing(viewing === item.id ? null : item.id)}
                >
                  {viewing === item.id ? "Fechar" : "Ver"}
                </Button>
              </div>
            ))
          )}
          {viewingItem && (
            <div className="space-y-4 rounded-md border p-4">
              <AnamnesisAnswerView anamnesis={viewingItem} />
              <Signature request={requestByAnamnesis.get(viewingItem.id)} />
            </div>
          )}
        </CardContent>
      </Card>

      <SendFormDialog
        companyId={companyId}
        patient={patient}
        models={activeModels}
        open={sendOpen}
        onOpenChange={setSendOpen}
      />
    </div>
  );
}
