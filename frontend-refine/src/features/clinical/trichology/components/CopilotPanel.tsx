"use client";

import { format } from "date-fns";
import { Lightbulb, ListChecks, TrendingUp } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { CopilotView } from "../types/trichology.types";
import { MEASURE_LABELS, type Measures } from "../types/trichology.types";

/** Copiloto do exame: pendências, hipóteses para investigar e evolução (doc v1.2, seção 3.3). */
export function CopilotPanel({
  copilot,
  loading,
}: {
  copilot: CopilotView | undefined;
  loading: boolean;
}) {
  if (loading || !copilot) {
    return <p className="text-sm text-muted-foreground">Carregando copiloto…</p>;
  }
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">Copiloto do exame</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 text-sm">
        <section className="space-y-1">
          <h4 className="flex items-center gap-1 font-medium">
            <ListChecks className="h-4 w-4" /> Pendências
          </h4>
          {copilot.pending.length === 0 ? (
            <p className="text-muted-foreground">Nada pendente.</p>
          ) : (
            <ul className="list-disc space-y-0.5 pl-5">
              {copilot.pending.map((p) => (
                <li key={p}>{p}</li>
              ))}
            </ul>
          )}
        </section>

        <section className="space-y-1">
          <h4 className="flex items-center gap-1 font-medium">
            <Lightbulb className="h-4 w-4" /> Hipóteses para investigar
          </h4>
          {copilot.hypotheses.length === 0 ? (
            <p className="text-muted-foreground">Ainda sem dados suficientes.</p>
          ) : (
            copilot.hypotheses.map((h) => (
              <div key={h.code} className="rounded-md border p-2">
                <p className="font-medium">{h.label}</p>
                <ul className="list-disc pl-5 text-xs text-muted-foreground">
                  {h.support.map((s) => (
                    <li key={s}>{s}</li>
                  ))}
                </ul>
              </div>
            ))
          )}
        </section>

        {copilot.evolution && (
          <section className="space-y-1">
            <h4 className="flex items-center gap-1 font-medium">
              <TrendingUp className="h-4 w-4" /> Evolução
            </h4>
            <p className="text-xs text-muted-foreground">
              Comparado ao exame publicado em{" "}
              {format(new Date(copilot.evolution.previousPublishedAt), "dd/MM/yyyy")}.
            </p>
            {copilot.evolution.previousRps !== null && copilot.evolution.currentRps !== null && (
              <p>
                RPS: {copilot.evolution.previousRps} → {copilot.evolution.currentRps}
              </p>
            )}
            {copilot.evolution.deltas.map((d) => (
              <p key={`${d.regionCode}-${d.measure}`}>
                {d.regionCode} · {MEASURE_LABELS[d.measure as keyof Measures] ?? d.measure}:{" "}
                {d.previous} → {d.current}
              </p>
            ))}
          </section>
        )}

        <p className="text-xs text-muted-foreground">{copilot.disclaimer}</p>
      </CardContent>
    </Card>
  );
}
