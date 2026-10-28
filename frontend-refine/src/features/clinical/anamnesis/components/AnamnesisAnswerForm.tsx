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
  complete: boolean;
}) {
  const saveMutation = useSavePatientAnamnesis(companyId, contactId);
  const [answers, setAnswers] = useState<PatientAnamnesisAnswers>(() => ({
    ...(anamnesis.answers ?? {}),
  }));

  const handleSubmit = () => {
    saveMutation.mutate({ anamnesisId: anamnesis.id, payload: { answers, complete } });
  };

  return (
    <div className="space-y-6">
      <AnamnesisFields
        sections={orderedSections(anamnesis.snapshot?.sections)}
        answers={answers}
        onChange={setAnswers}
      />
      <Button onClick={handleSubmit} disabled={saveMutation.isPending}>
        {complete ? "Concluir" : "Salvar rascunho"}
      </Button>
    </div>
  );
}
