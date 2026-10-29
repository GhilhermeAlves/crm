"use client";

import { DateTimePicker } from "@/components/ui/date-time-picker";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import type {
  AnamnesisQuestion,
  AnamnesisSection,
} from "@/features/masterdata/anamnesis/types/anamnesis.types";
import type {
  PatientAnamnesisAnswer,
  PatientAnamnesisAnswers,
} from "../types/patient-anamnesis.types";

function shouldShowComplement(question: AnamnesisQuestion, value: unknown) {
  if (!question.allowComplement) return false;
  if (question.complementTrigger === "ALWAYS") return true;
  if (question.complementTrigger === "YES") return value === "YES";
  if (question.complementTrigger === "NO") return value === "NO";
  if (question.complementTrigger === "OPTION") {
    if (Array.isArray(value)) return value.includes(question.complementOptionValue);
    return value === question.complementOptionValue;
  }
  return false;
}

const bySortOrder = <T extends { sortOrder: number }>(a: T, b: T) => a.sortOrder - b.sortOrder;

/** Seções na ordem de preenchimento: primeiro as do paciente, depois as do profissional. */
export function orderedSections(
  sections: AnamnesisSection[] | undefined,
  includeProfessional = true,
): AnamnesisSection[] {
  const sorted = [...(sections ?? [])].sort(bySortOrder);
  const general = sorted.filter((s) => !s.professional);
  return includeProfessional ? [...general, ...sorted.filter((s) => s.professional)] : general;
}

/** Perguntas obrigatórias ainda sem resposta (texto da pergunta). */
export function missingRequired(
  sections: AnamnesisSection[],
  answers: PatientAnamnesisAnswers,
): string[] {
  return sections.flatMap((section) =>
    (section.questions ?? [])
      .filter((q) => q.required)
      .filter((q) => {
        const value = answers[q.id]?.value;
        return (
          value === undefined ||
          value === null ||
          value === "" ||
          (Array.isArray(value) && value.length === 0)
        );
      })
      .map((q) => q.text),
  );
}

/**
 * Campos de uma anamnese (sem botões). Usado na ficha do paciente e no
 * formulário público que o paciente responde pelo link.
 */
export function AnamnesisFields({
  sections,
  answers,
  onChange,
  size = "default",
}: {
  sections: AnamnesisSection[];
  answers: PatientAnamnesisAnswers;
  onChange: (answers: PatientAnamnesisAnswers) => void;
  /** "large" aumenta toques e textos para celular/tablet. */
  size?: "default" | "large";
}) {
  const large = size === "large";

  const update = (questionId: string, patch: Partial<PatientAnamnesisAnswer>) => {
    const prev = answers[questionId] ?? { value: null, complement: null };
    onChange({ ...answers, [questionId]: { ...prev, ...patch } });
  };

  const optionClass = large ? "flex items-center gap-3 py-1 text-base" : "flex items-center gap-2";

  return (
    <div className="space-y-6">
      {sections.map((section) => (
        <div key={section.id} className="space-y-4">
          <h3 className={large ? "text-lg font-semibold" : "text-base font-semibold"}>
            {section.title}
          </h3>
          {section.professional && (
            <p className="text-sm text-muted-foreground">Preenchimento do profissional</p>
          )}
          {[...(section.questions ?? [])].sort(bySortOrder).map((question) => {
            const ans = answers[question.id];
            const value = ans?.value ?? null;
            const text =
              typeof value === "string" || typeof value === "number" ? String(value) : "";
            const options = [...(question.options ?? [])].sort(bySortOrder);

            return (
              <div key={question.id} className="space-y-2 rounded-md border p-4">
                <Label className={large ? "text-base" : undefined}>
                  {question.text}
                  {question.required ? <span className="text-destructive"> *</span> : null}
                </Label>
                {question.type === "SHORT_TEXT" && (
                  <Input
                    value={text}
                    onChange={(e) => update(question.id, { value: e.target.value })}
                  />
                )}
                {question.type === "LONG_TEXT" && (
                  <Textarea
                    value={text}
                    onChange={(e) => update(question.id, { value: e.target.value })}
                  />
                )}
                {question.type === "NUMBER" && (
                  <Input
                    type="number"
                    inputMode="decimal"
                    value={text}
                    onChange={(e) => update(question.id, { value: e.target.value })}
                  />
                )}
                {question.type === "DATE" && (
                  <DateTimePicker
                    dateOnly
                    disableFuture
                    fromYear={1920}
                    placeholder="dd/mm/aaaa"
                    value={text || undefined}
                    onChange={(v) => update(question.id, { value: v })}
                  />
                )}
                {(question.type === "YES_NO" || question.type === "YES_NO_WITH_TEXT") && (
                  <RadioGroup
                    value={text}
                    onValueChange={(v) => update(question.id, { value: v })}
                    className="flex gap-6"
                  >
                    {[
                      ["YES", "Sim"],
                      ["NO", "Não"],
                    ].map(([v, label]) => (
                      <div key={v} className={optionClass}>
                        <RadioGroupItem value={v} id={`${question.id}-${v}`} />
                        <Label htmlFor={`${question.id}-${v}`}>{label}</Label>
                      </div>
                    ))}
                  </RadioGroup>
                )}
                {question.type === "SINGLE_SELECT" && (
                  <RadioGroup
                    value={text}
                    onValueChange={(v) => update(question.id, { value: v })}
                    className="space-y-2"
                  >
                    {options.map((opt) => (
                      <div key={opt.id} className={optionClass}>
                        <RadioGroupItem value={opt.value} id={`${question.id}-${opt.id}`} />
                        <Label htmlFor={`${question.id}-${opt.id}`}>{opt.label}</Label>
                      </div>
                    ))}
                  </RadioGroup>
                )}
                {question.type === "MULTI_SELECT" && (
                  <div className="space-y-2">
                    {options.map((opt) => {
                      const current = Array.isArray(value) ? (value as string[]) : [];
                      return (
                        <div key={opt.id} className={optionClass}>
                          <Checkbox
                            id={`${question.id}-${opt.id}`}
                            checked={current.includes(opt.value)}
                            onCheckedChange={(checked) =>
                              update(question.id, {
                                value: checked
                                  ? [...current.filter((v) => v !== opt.value), opt.value]
                                  : current.filter((v) => v !== opt.value),
                              })
                            }
                          />
                          <Label htmlFor={`${question.id}-${opt.id}`}>{opt.label}</Label>
                        </div>
                      );
                    })}
                  </div>
                )}
                {shouldShowComplement(question, value) && (
                  <div className="space-y-2">
                    <Label>{question.complementLabel || "Complemento"}</Label>
                    <Input
                      value={ans?.complement ?? ""}
                      onChange={(e) => update(question.id, { complement: e.target.value })}
                    />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
