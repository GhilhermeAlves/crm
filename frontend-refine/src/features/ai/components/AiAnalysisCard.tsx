"use client";

import { BrainCircuit, Database, ListChecks, Loader2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import type { AiAnalysisResponse } from "../types/ai.types";

type Props = {
  /** Resultado da análise; null enquanto carrega ou quando falhou. */
  analysis: AiAnalysisResponse | null;
  /** Estado de carregamento da análise (AI-06 §10). */
  loading: boolean;
  /** Mensagem de erro amigável, nunca stack trace (AI-06 §11). */
  error: string | null;
};

/**
 * Cartão de análise contextual (AI-06). Apresenta, de forma visualmente
 * diferenciada, Resumo, Fatos (dados reais do CRM), Inferências (conclusões da
 * IA) e Recomendações (sugestões — NUNCA executadas). Reutiliza o design system
 * (Card/Badge/Separator/Skeleton) e trata loading, erro e empty states sem
 * inventar padrão visual novo.
 */
export function AiAnalysisCard({ analysis, loading, error }: Props) {
  return (
    <Card
      className="border-primary/20 bg-primary/[0.02]"
      aria-label="Análise contextual do registro"
    >
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <BrainCircuit className="h-4 w-4 text-primary" />
          Análise contextual
        </CardTitle>
      </CardHeader>

      {loading && (
        <CardContent
          className="space-y-3"
          role="status"
          aria-live="polite"
          aria-label="Analisando registro"
        >
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Analisando o registro atual...
          </p>
          <div className="space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-5/6" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        </CardContent>
      )}

      {!loading && error && (
        <CardContent className="rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
          {error}
        </CardContent>
      )}

      {!loading && !error && analysis && (
        <CardContent className="space-y-4">
          {/* Resumo */}
          <section aria-label="Resumo da análise">
            <p className="whitespace-pre-wrap break-words text-sm">{analysis.summary}</p>
          </section>

          <Separator />

          {/* Fatos */}
          <section aria-label="Fatos">
            <h4 className="mb-2 flex items-center gap-2 text-sm font-semibold">
              <Database className="h-4 w-4 text-muted-foreground" />
              <Badge variant="secondary">Fatos</Badge>
              <span className="text-xs font-normal text-muted-foreground">Dados reais do CRM</span>
            </h4>
            {analysis.facts.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Não há dados suficientes para avaliar.
              </p>
            ) : (
              <ul className="space-y-1.5">
                {analysis.facts.map((fact) => (
                  <li key={fact.key} className="text-sm">
                    <span className="font-medium">{fact.label}</span>
                    <span className="whitespace-pre-wrap break-words text-muted-foreground">
                      : {fact.value}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <Separator />

          {/* Inferências */}
          <section aria-label="Inferências">
            <h4 className="mb-2 flex items-center gap-2 text-sm font-semibold">
              <BrainCircuit className="h-4 w-4 text-muted-foreground" />
              <Badge variant="outline">Inferências</Badge>
              <span className="text-xs font-normal text-muted-foreground">Interpretação da IA</span>
            </h4>
            {analysis.inferences.length === 0 ? (
              <p className="text-sm text-muted-foreground">Sem inferências para este registro.</p>
            ) : (
              <ul className="space-y-1.5">
                {analysis.inferences.map((inference) => (
                  <li key={inference.key} className="flex items-start gap-2 text-sm">
                    <span className="whitespace-pre-wrap break-words">
                      {inference.text}
                      {inference.confidence != null && (
                        <span className="ml-1 text-xs text-muted-foreground">
                          (confiança {inference.confidence})
                        </span>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <Separator />

          {/* Recomendações */}
          <section aria-label="Recomendações">
            <h4 className="mb-2 flex items-center gap-2 text-sm font-semibold">
              <ListChecks className="h-4 w-4 text-muted-foreground" />
              <Badge>Recomendações</Badge>
              <span className="text-xs font-normal text-muted-foreground">
                Sugestões — não executadas
              </span>
            </h4>
            {analysis.recommendations.length === 0 ? (
              <p className="text-sm text-muted-foreground">Sem recomendações por enquanto.</p>
            ) : (
              <ul className="space-y-3">
                {analysis.recommendations.map((recommendation) => (
                  <li key={recommendation.key} className="space-y-0.5">
                    <div className="flex items-center gap-2 text-sm">
                      <span className="font-medium">{recommendation.title}</span>
                      {recommendation.priority != null && (
                        <Badge variant="secondary">Prioridade {recommendation.priority}</Badge>
                      )}
                    </div>
                    {recommendation.description && (
                      <p className="whitespace-pre-wrap break-words text-sm text-muted-foreground">
                        {recommendation.description}
                      </p>
                    )}
                    {recommendation.justification && (
                      <p className="whitespace-pre-wrap break-words text-xs text-muted-foreground/80">
                        {recommendation.justification}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </CardContent>
      )}
    </Card>
  );
}
