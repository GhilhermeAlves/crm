"use client";

import { useEffect, useState } from "react";
import { format } from "date-fns";
import { CheckCircle2, Loader2, ShieldAlert, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PublicTrichologyService } from "../services/trichology.service";
import type { PublicQuizView, QuizAnswers, VerifyView } from "../types/trichology.types";
import { QuizForm, missingRequired } from "./QuizForm";

function failureMessage(status: number): string {
  if (status === 429) return "Muitas tentativas. Aguarde alguns minutos e tente de novo.";
  if (status === 404) return "Link inválido ou expirado. Peça um novo link à clínica.";
  return "Não foi possível abrir agora. Tente de novo mais tarde.";
}

/** Quiz capilar respondido pelo paciente pelo link (sem login). */
export function PublicQuiz({ token }: { token: string }) {
  const [view, setView] = useState<PublicQuizView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [answers, setAnswers] = useState<QuizAnswers>({});
  const [showMissing, setShowMissing] = useState(false);
  const [website, setWebsite] = useState("");
  const [sending, setSending] = useState(false);
  const [done, setDone] = useState(false);

  useEffect(() => {
    void PublicTrichologyService.quiz(token).then((result) => {
      if (result.ok) setView(result.data);
      else setError(failureMessage(result.status));
    });
  }, [token]);

  const submit = async () => {
    if (!view) return;
    if (missingRequired(view.quiz.questions, answers).length > 0) {
      setShowMissing(true);
      return;
    }
    setSending(true);
    const result = await PublicTrichologyService.submitQuiz(token, answers, website);
    setSending(false);
    if (result.ok) setDone(true);
    else setError(result.message ?? failureMessage(result.status));
  };

  return (
    <Card className="mx-auto max-w-2xl">
      <CardContent className="space-y-5 p-6">
        {error ? (
          <p className="text-center text-sm text-destructive">{error}</p>
        ) : done ? (
          <div className="space-y-2 text-center">
            <CheckCircle2 className="mx-auto h-10 w-10 text-emerald-600" />
            <p className="font-medium">Respostas enviadas. Obrigado!</p>
            <p className="text-sm text-muted-foreground">
              A clínica vai usar suas respostas na avaliação.
            </p>
          </div>
        ) : !view ? (
          <p className="text-center text-sm text-muted-foreground">Carregando…</p>
        ) : (
          <>
            <div>
              <p className="text-sm text-muted-foreground">{view.clinicName}</p>
              <h1 className="text-xl font-semibold">
                Questionário capilar{view.patientFirstName ? `, ${view.patientFirstName}` : ""}
              </h1>
              <p className="text-sm text-muted-foreground">
                Responda antes da consulta. Leva cerca de 3 minutos.
              </p>
            </div>
            <QuizForm
              questions={view.quiz.questions}
              answers={answers}
              onChange={setAnswers}
              showMissing={showMissing}
            />
            {/* Campo isca contra robôs: fica escondido para pessoas. */}
            <Input
              tabIndex={-1}
              autoComplete="off"
              aria-hidden="true"
              className="hidden"
              value={website}
              onChange={(e) => setWebsite(e.target.value)}
            />
            {showMissing && missingRequired(view.quiz.questions, answers).length > 0 && (
              <p className="text-sm text-destructive">Responda as perguntas marcadas.</p>
            )}
            <Button
              type="button"
              className="w-full"
              onClick={() => void submit()}
              disabled={sending}
            >
              {sending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Enviar respostas
            </Button>
          </>
        )}
      </CardContent>
    </Card>
  );
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
