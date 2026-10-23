"use client";

import { useState } from "react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { useAnamnesisModels } from "@/features/masterdata/anamnesis/hooks/useAnamnesis";
import {
  useCurrentPatientAnamnesis,
  useDiscardPatientAnamnesis,
  usePatientAnamneses,
  useStartPatientAnamnesis,
} from "../hooks/usePatientAnamnesis";
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
import { format, parseISO } from "date-fns";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

export function PatientAnamnesisTab({
  companyId,
  contactId,
  patient,
}: {
  companyId: string;
  contactId: string;
  patient: Contact;
}) {
  const { user } = useAuth();
  const can = usePermission();
  const canUpdate = can("anamnesis:manage") || can("contact:update");
  const { data: models = [] } = useAnamnesisModels(companyId);
  const { data: current, isLoading: loadingCurrent } = useCurrentPatientAnamnesis(
    companyId,
    contactId,
  );
  const { data: history = [] } = usePatientAnamneses(companyId, contactId);
  const startMutation = useStartPatientAnamnesis(companyId, contactId);
  const discardMutation = useDiscardPatientAnamnesis(companyId, contactId);
  const [selectedModel, setSelectedModel] = useState<string | undefined>(undefined);
  const [viewing, setViewing] = useState<string | null>(null);

  const activeModels = models.filter((m) => m.active);
  const defaultModel = activeModels.find((m) => m.isDefault) || activeModels[0];
  const effectiveModel = selectedModel || defaultModel?.id;

  const draft =
    history.find((h) => h.status === "DRAFT" && h.createdBy === user?.id) ||
    history.find((h) => h.status === "DRAFT");

  const completedHistory = history.filter((h) => h.status === "COMPLETED" && h.id !== current?.id);

  if (loadingCurrent) {
    return <p className="text-sm text-muted-foreground">Carregando...</p>;
  }

  if (draft) {
    return (
      <div className="space-y-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Anamnese em rascunho</CardTitle>
            <div className="flex gap-2">
              <AnamnesisAnswerForm
                companyId={companyId}
                contactId={contactId}
                anamnesis={draft}
                complete={false}
              />
              <AnamnesisAnswerForm
                companyId={companyId}
                contactId={contactId}
                anamnesis={draft}
                complete={true}
              />
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button variant="outline">Descartar</Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Descartar anamnese?</AlertDialogTitle>
                    <AlertDialogDescription>
                      Esta ação não pode ser desfeita.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancelar</AlertDialogCancel>
                    <AlertDialogAction onClick={() => discardMutation.mutate(draft.id)}>
                      Descartar
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </div>
          </CardHeader>
        </Card>
        <AnamnesisAnswerForm
          companyId={companyId}
          contactId={contactId}
          anamnesis={draft}
          complete={false}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Anamnese atual</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {current ? (
            <>
              <p className="text-sm text-muted-foreground">
                Preenchida por {current.completedByName || "—"} em{" "}
                {current.completedAt ? format(parseISO(current.completedAt), "dd/MM/yyyy") : "—"}
              </p>
              <AnamnesisAnswerView anamnesis={current} />
              <Button onClick={() => setViewing(null)}>Nova anamnese</Button>
            </>
          ) : (
            <>
              <p className="text-sm text-muted-foreground">Nenhuma anamnese preenchida.</p>
              {activeModels.length > 0 && canUpdate ? (
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
                    disabled={!effectiveModel || startMutation.isPending}
                    onClick={() =>
                      effectiveModel && startMutation.mutate({ modelId: effectiveModel })
                    }
                  >
                    Iniciar anamnese
                  </Button>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">
                  Nenhum modelo de anamnese ativo disponível.
                </p>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Histórico</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
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
                      {item.completedAt
                        ? format(parseISO(item.completedAt), "dd/MM/yyyy HH:mm")
                        : "—"}{" "}
                      • <Badge variant="secondary">Concluída</Badge>
                    </p>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => setViewing(item.id)}>
                    Ver
                  </Button>
                </div>
              ))
            )}
          </div>
        </CardContent>
      </Card>

      {viewing && history.find((h) => h.id === viewing) && (
        <Card>
          <CardHeader>
            <CardTitle>Histórico</CardTitle>
          </CardHeader>
          <CardContent>
            <AnamnesisAnswerView anamnesis={history.find((h) => h.id === viewing)!} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
