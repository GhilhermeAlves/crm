"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useSavePatientAnamnesis } from "../hooks/usePatientAnamnesis";
import type {
  PatientAnamnesisAnswers,
  PatientAnamnesisResponse,
} from "../types/patient-anamnesis.types";
import { AnamnesisFields, orderedSections } from "./AnamnesisFields";

export function AnamnesisAnswerForm({
  companyId,
  contactId,
  anamnesis,
  complete,
}: {
  companyId: string;
  contactId: string;
  anamnesis: PatientAnamnesisResponse;
  /** Só um botão (rascunho ou concluir); sem o prop mostra os dois. */
  complete?: boolean;
}) {
  const saveMutation = useSavePatientAnamnesis(companyId, contactId);
  const [answers, setAnswers] = useState<PatientAnamnesisAnswers>(() => ({
    ...(anamnesis.answers ?? {}),
  }));

  const submit = (finish: boolean) => {
    saveMutation.mutate({ anamnesisId: anamnesis.id, payload: { answers, complete: finish } });
  };

  return (
    <div className="space-y-6">
      <AnamnesisFields
        sections={orderedSections(anamnesis.snapshot?.sections)}
        answers={answers}
        onChange={setAnswers}
      />
      <div className="flex flex-wrap gap-2">
        {complete !== true && (
          <Button
            variant={complete === undefined ? "outline" : "default"}
            onClick={() => submit(false)}
            disabled={saveMutation.isPending}
          >
            Salvar rascunho
          </Button>
        )}
        {complete !== false && (
          <Button onClick={() => submit(true)} disabled={saveMutation.isPending}>
            Concluir
          </Button>
        )}
      </div>
    </div>
  );
}
