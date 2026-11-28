"use client";

import { useId } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import type { QuizAnswers, QuizQuestion } from "../types/trichology.types";

/** Perguntas visíveis pela ramificação (mesma regra do QuizEngine no backend). */
export function visibleQuestions(questions: QuizQuestion[], answers: QuizAnswers): QuizQuestion[] {
  return questions.filter((q) => {
    if (!q.showIf) return true;
    const dep = answers[q.showIf.dependsOn];
    return dep !== undefined && q.showIf.values.includes(String(dep));
  });
}

function isEmpty(value: QuizAnswers[string] | undefined): boolean {
  return (
    value === undefined ||
    value === null ||
    (typeof value === "string" && value.trim() === "") ||
    (Array.isArray(value) && value.length === 0)
  );
}

export function missingRequired(questions: QuizQuestion[], answers: QuizAnswers): QuizQuestion[] {
  return visibleQuestions(questions, answers).filter((q) => q.required && isEmpty(answers[q.id]));
}

type Props = {
  questions: QuizQuestion[];
  answers: QuizAnswers;
  onChange: (answers: QuizAnswers) => void;
  disabled?: boolean;
  showMissing?: boolean;
};

export function QuizForm({ questions, answers, onChange, disabled, showMissing }: Props) {
  const uid = useId();
  const missing = new Set(showMissing ? missingRequired(questions, answers).map((q) => q.id) : []);
  const set = (id: string, value: QuizAnswers[string] | undefined) => {
    const next = { ...answers };
    if (value === undefined) delete next[id];
    else next[id] = value;
    onChange(next);
  };

  return (
    <div className="space-y-5">
      {visibleQuestions(questions, answers).map((q) => {
        const id = `${uid}-${q.id}`;
        const value = answers[q.id];
        return (
          <fieldset key={q.id} className="space-y-2" disabled={disabled}>
            <legend className="text-sm font-medium">
              {q.text}
              {q.required && <span className="text-destructive"> *</span>}
            </legend>
            {q.type === "SINGLE" && (
              <RadioGroup
                value={typeof value === "string" ? value : ""}
                onValueChange={(v) => set(q.id, v)}
                className="grid gap-2 sm:grid-cols-2"
              >
                {q.options.map((o) => (
                  <div key={o.value} className="flex items-center gap-2">
                    <RadioGroupItem id={`${id}-${o.value}`} value={o.value} />
                    <Label htmlFor={`${id}-${o.value}`} className="font-normal">
                      {o.label}
                    </Label>
                  </div>
                ))}
              </RadioGroup>
            )}
            {q.type === "MULTI" && (
              <div className="grid gap-2 sm:grid-cols-2">
                {q.options.map((o) => {
                  const list = Array.isArray(value) ? value : [];
                  const checked = list.includes(o.value);
                  return (
                    <div key={o.value} className="flex items-center gap-2">
                      <Checkbox
                        id={`${id}-${o.value}`}
                        checked={checked}
                        onCheckedChange={(c) => {
                          // "Nenhum/Nada" exclui as outras opções e vice-versa.
                          const exclusive = o.value === "NONE";
                          const base = exclusive ? [] : list.filter((v) => v !== "NONE");
                          const next = c ? [...base, o.value] : list.filter((v) => v !== o.value);
                          set(q.id, next.length ? next : undefined);
                        }}
                      />
                      <Label htmlFor={`${id}-${o.value}`} className="font-normal">
                        {o.label}
                      </Label>
                    </div>
                  );
                })}
              </div>
            )}
            {q.type === "BOOLEAN" && (
              <RadioGroup
                value={value === true ? "yes" : value === false ? "no" : ""}
                onValueChange={(v) => set(q.id, v === "yes")}
                className="flex gap-6"
              >
                <div className="flex items-center gap-2">
                  <RadioGroupItem id={`${id}-yes`} value="yes" />
                  <Label htmlFor={`${id}-yes`} className="font-normal">
                    Sim
                  </Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem id={`${id}-no`} value="no" />
                  <Label htmlFor={`${id}-no`} className="font-normal">
                    Não
                  </Label>
                </div>
              </RadioGroup>
            )}
            {q.type === "TEXT" && (
              <Textarea
                id={id}
                value={typeof value === "string" ? value : ""}
                maxLength={2000}
                onChange={(e) => set(q.id, e.target.value || undefined)}
              />
            )}
            {q.type === "NUMBER" && (
              <Input
                id={id}
                type="number"
                value={typeof value === "number" ? value : ""}
                onChange={(e) =>
                  set(q.id, e.target.value === "" ? undefined : Number(e.target.value))
                }
              />
            )}
            {missing.has(q.id) && (
              <p className="text-xs text-destructive">Responda esta pergunta.</p>
            )}
          </fieldset>
        );
      })}
    </div>
  );
}
