"use client";

import type { PatientAnamnesisResponse } from "../types/patient-anamnesis.types";

export function AnamnesisAnswerView({ anamnesis }: { anamnesis: PatientAnamnesisResponse }) {
  const sections = [...(anamnesis.snapshot?.sections || [])].sort(
    (a, b) => a.sortOrder - b.sortOrder,
  );
  const professional = sections.filter((s) => s.professional);
  const general = sections.filter((s) => !s.professional);
  const ordered = [...general, ...professional];
  const answers = new Map((anamnesis.answers || []).map((a) => [a.questionId, a]));

  return (
    <div className="space-y-6">
      {ordered.map((section) => (
        <div key={section.id} className="space-y-3">
          <h3 className="text-base font-semibold">{section.title}</h3>
          {[...(section.questions || [])]
            .sort((a, b) => a.sortOrder - b.sortOrder)
            .map((question) => {
              const ans = answers.get(question.id);
              const value = ans?.valueJson ?? ans?.valueText;
              let display = value;
              if (Array.isArray(value)) {
                display = value
                  .map((v) => {
                    const opt = (question.options || []).find((o) => o.value === v);
                    return opt?.label || v;
                  })
                  .join(", ");
              } else if (value === "YES") display = "Sim";
              else if (value === "NO") display = "Não";
              else if (typeof value === "string" && question.options?.length) {
                const opt = question.options.find((o) => o.value === value);
                if (opt) display = opt.label;
              }
              if (display === null || display === undefined || display === "") return null;
              return (
                <div key={question.id} className="space-y-1">
                  <p className={`text-sm ${question.highlight ? "text-destructive" : ""}`}>
                    {question.text}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {String(display)}
                    {ans?.complement ? ` - ${ans.complement}` : ""}
                  </p>
                </div>
              );
            })}
        </div>
      ))}
    </div>
  );
}
