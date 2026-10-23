"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import type { AnamnesisQuestion } from "@/features/masterdata/anamnesis/types/anamnesis.types";
import { useSavePatientAnamnesis } from "../hooks/usePatientAnamnesis";
import type {
  PatientAnamnesisAnswer,
  PatientAnamnesisResponse,
} from "../types/patient-anamnesis.types";

function shouldShowComplement(question: AnamnesisQuestion, value: unknown) {
  if (!question.allowComplement) return false;
  if (question.complementTrigger === "ALWAYS") return true;
  if (question.complementTrigger === "YES") return value === "YES";
  if (question.complementTrigger === "NO") return value === "NO";
  if (question.complementTrigger === "OPTION") {
    if (Array.isArray(value)) {
      return value.includes(question.complementOptionValue);
    }
    return value === question.complementOptionValue;
  }
  return false;
}

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
  const [answers, setAnswers] = useState<Record<string, PatientAnamnesisAnswer>>(() => {
    const map: Record<string, PatientAnamnesisAnswer> = {};
    for (const ans of anamnesis.answers || []) {
      map[ans.questionId] = ans;
    }
    return map;
  });

  const updateAnswer = (
    questionId: string,
    updater: (prev: PatientAnamnesisAnswer) => PatientAnamnesisAnswer,
  ) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: updater(
        prev[questionId] || {
          questionId,
          valueText: null,
          valueJson: null,
          complement: null,
        },
      ),
    }));
  };

  const handleSubmit = () => {
    const payload = Object.values(answers);
    saveMutation.mutate({ anamnesisId: anamnesis.id, payload: { answers: payload, complete } });
  };

  const sections = [...(anamnesis.snapshot?.sections || [])].sort(
    (a, b) => a.sortOrder - b.sortOrder,
  );
  const professional = sections.filter((s) => s.professional);
  const general = sections.filter((s) => !s.professional);
  const ordered = [...general, ...professional];

  return (
    <div className="space-y-6">
      {ordered.map((section) => (
        <div key={section.id} className="space-y-4">
          <h3 className="text-base font-semibold">{section.title}</h3>
          {section.professional && (
            <p className="text-sm text-muted-foreground">Preenchimento do profissional</p>
          )}
          {[...(section.questions || [])]
            .sort((a, b) => a.sortOrder - b.sortOrder)
            .map((question) => {
              const ans = answers[question.id];
              const value = ans?.valueJson ?? ans?.valueText ?? null;
              const showComplement = shouldShowComplement(question, value);

              return (
                <div key={question.id} className="space-y-2 rounded-md border p-4">
                  <Label>
                    {question.text}
                    {question.required ? <span className="text-destructive"> *</span> : null}
                  </Label>
                  {question.type === "SHORT_TEXT" && (
                    <Input
                      value={(ans?.valueText as string) || ""}
                      onChange={(e) =>
                        updateAnswer(question.id, (prev) => ({
                          ...prev,
                          valueText: e.target.value,
                          valueJson: e.target.value,
                        }))
                      }
                    />
                  )}
                  {question.type === "LONG_TEXT" && (
                    <Textarea
                      value={(ans?.valueText as string) || ""}
                      onChange={(e) =>
                        updateAnswer(question.id, (prev) => ({
                          ...prev,
                          valueText: e.target.value,
                          valueJson: e.target.value,
                        }))
                      }
                    />
                  )}
                  {question.type === "NUMBER" && (
                    <Input
                      type="number"
                      value={(ans?.valueText as string) || ""}
                      onChange={(e) =>
                        updateAnswer(question.id, (prev) => ({
                          ...prev,
                          valueText: e.target.value,
                          valueJson: e.target.value,
                        }))
                      }
                    />
                  )}
                  {(question.type === "YES_NO" || question.type === "YES_NO_WITH_TEXT") && (
                    <RadioGroup
                      value={(value as string) || ""}
                      onValueChange={(v) =>
                        updateAnswer(question.id, (prev) => ({
                          ...prev,
                          valueText: v,
                          valueJson: v,
                        }))
                      }
                      className="flex gap-4"
                    >
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="YES" id={`${question.id}-yes`} />
                        <Label htmlFor={`${question.id}-yes`}>Sim</Label>
                      </div>
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="NO" id={`${question.id}-no`} />
                        <Label htmlFor={`${question.id}-no`}>Não</Label>
                      </div>
                    </RadioGroup>
                  )}
                  {question.type === "SINGLE_SELECT" && (
                    <RadioGroup
                      value={(value as string) || ""}
                      onValueChange={(v) =>
                        updateAnswer(question.id, (prev) => ({
                          ...prev,
                          valueText: v,
                          valueJson: v,
                        }))
                      }
                      className="space-y-2"
                    >
                      {[...(question.options || [])]
                        .sort((a, b) => a.sortOrder - b.sortOrder)
                        .map((opt) => (
                          <div key={opt.id} className="flex items-center gap-2">
                            <RadioGroupItem value={opt.value} id={`${question.id}-${opt.id}`} />
                            <Label htmlFor={`${question.id}-${opt.id}`}>{opt.label}</Label>
                          </div>
                        ))}
                    </RadioGroup>
                  )}
                  {question.type === "MULTI_SELECT" && (
                    <div className="space-y-2">
                      {[...(question.options || [])]
                        .sort((a, b) => a.sortOrder - b.sortOrder)
                        .map((opt) => {
                          const selected = Array.isArray(value) ? value.includes(opt.value) : false;
                          return (
                            <div key={opt.id} className="flex items-center gap-2">
                              <Checkbox
                                id={`${question.id}-${opt.id}`}
                                checked={selected}
                                onCheckedChange={(checked) => {
                                  updateAnswer(question.id, (prev) => {
                                    const current = Array.isArray(prev.valueJson)
                                      ? [...(prev.valueJson as string[])]
                                      : [];
                                    if (checked) {
                                      if (!current.includes(opt.value)) current.push(opt.value);
                                    } else {
                                      const idx = current.indexOf(opt.value);
                                      if (idx >= 0) current.splice(idx, 1);
                                    }
                                    return {
                                      ...prev,
                                      valueJson: current,
                                      valueText: current.join(", "),
                                    };
                                  });
                                }}
                              />
                              <Label htmlFor={`${question.id}-${opt.id}`}>{opt.label}</Label>
                            </div>
                          );
                        })}
                    </div>
                  )}
                  {showComplement && (
                    <div className="space-y-2">
                      <Label>{question.complementLabel || "Complemento"}</Label>
                      <Input
                        value={ans?.complement || ""}
                        onChange={(e) =>
                          updateAnswer(question.id, (prev) => ({
                            ...prev,
                            complement: e.target.value,
                          }))
                        }
                      />
                    </div>
                  )}
                </div>
              );
            })}
        </div>
      ))}
      <Button onClick={handleSubmit} disabled={saveMutation.isPending}>
        {complete ? "Concluir" : "Salvar rascunho"}
      </Button>
    </div>
  );
}
