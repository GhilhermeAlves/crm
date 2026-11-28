"use client";

import { useEffect, useState } from "react";
import { format } from "date-fns";
import { ShieldAlert, ShieldCheck } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { PublicTrichologyService } from "../services/trichology.service";
import type { VerifyView } from "../types/trichology.types";

function failureMessage(status: number): string {
  if (status === 429) return "Muitas tentativas. Aguarde alguns minutos e tente de novo.";
  if (status === 404) return "Link inválido ou expirado. Peça um novo link à clínica.";
  return "Não foi possível abrir agora. Tente de novo mais tarde.";
}

/** Verificação pública do relatório pelo código: sem dados clínicos nem do paciente. */
export function VerifyReport({ code }: { code: string }) {
  const [result, setResult] = useState<VerifyView | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void PublicTrichologyService.verify(code).then((r) => {
      if (r.ok) setResult(r.data);
      else setError(failureMessage(r.status));
    });
  }, [code]);

  return (
    <Card className="mx-auto max-w-lg">
      <CardContent className="space-y-3 p-6 text-sm">
        <h1 className="text-lg font-semibold">Verificação de relatório de tricologia</h1>
        <p className="text-muted-foreground">
          Código: <code>{code.toUpperCase()}</code>
        </p>
        {error && <p className="text-destructive">{error}</p>}
        {!error && !result && <p className="text-muted-foreground">Verificando…</p>}
        {result && !result.valid && (
          <p className="flex items-center gap-2 text-destructive">
            <ShieldAlert className="h-5 w-5" /> Nenhum relatório publicado com este código.
          </p>
        )}
        {result?.valid && (
          <div className="space-y-2">
            <p className="flex items-center gap-2 font-medium text-emerald-700 dark:text-emerald-400">
              <ShieldCheck className="h-5 w-5" /> Relatório autêntico
            </p>
            <p>Clínica: {result.clinicName}</p>
            <p>
              Profissional: {result.professionalName}
              {result.professionalRegistry ? ` — ${result.professionalRegistry}` : ""}
            </p>
            {result.publishedAt && (
              <p>Publicado em {format(new Date(result.publishedAt), "dd/MM/yyyy HH:mm")}</p>
            )}
            <p>Versão {result.reportVersion}</p>
            {result.superseded && (
              <p className="text-amber-700 dark:text-amber-400">
                Existe uma versão mais nova deste relatório; peça a atual à clínica.
              </p>
            )}
            <p className="break-all text-xs text-muted-foreground">
              Impressão digital do PDF (SHA-256): {result.pdfSha256}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
