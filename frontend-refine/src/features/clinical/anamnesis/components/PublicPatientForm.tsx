"use client";

import { useCallback, useEffect, useState } from "react";
import { CheckCircle2, Loader2, Maximize2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { type PublicForm, PublicFormService } from "../services/public-form.service";
import type { PatientAnamnesisAnswers } from "../types/patient-anamnesis.types";
import { AnamnesisFields, missingRequired, orderedSections } from "./AnamnesisFields";
import { SignaturePad } from "./SignaturePad";

const GENERIC_MESSAGE = "Não foi possível abrir o formulário. Tente novamente em instantes.";
const RATE_LIMIT_MESSAGE = "Muitas tentativas. Aguarde alguns minutos e tente de novo.";

function StatusCard({ title, text, done }: { title: string; text: string; done?: boolean }) {
  return (
    <Card className="m-auto w-full max-w-md border-crm-border bg-crm-surface">
      <CardContent className="space-y-3 pt-6 text-center">
        {done && <CheckCircle2 className="mx-auto h-12 w-12 text-primary" />}
        <h1 className="text-xl font-semibold">{title}</h1>
        <p className="text-sm text-muted-foreground">{text}</p>
      </CardContent>
    </Card>
  );
}

type State =
  | { kind: "loading" }
  | { kind: "error"; title: string; message: string }
  | { kind: "ready"; form: PublicForm }
  | { kind: "done" };

/**
 * Formulário que o paciente responde pelo link (sem login) ou no tablet da
 * recepção ({@code reception}): perguntas do paciente e assinatura no fim.
 */
export function PublicPatientForm({ token, reception }: { token: string; reception: boolean }) {
  const [state, setState] = useState<State>({ kind: "loading" });
  const [answers, setAnswers] = useState<PatientAnamnesisAnswers>({});
  const [signature, setSignature] = useState<string | null>(null);
  const [website, setWebsite] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    void PublicFormService.get(token).then((result) => {
      if (!active) return;
      if (result.ok) {
        setState({ kind: "ready", form: result.data });
      } else if (result.status === 410) {
        setState({
          kind: "error",
          title: "Formulário indisponível",
          message: result.message ?? "Este formulário já foi respondido ou o link expirou.",
        });
      } else if (result.status === 404) {
        setState({
          kind: "error",
          title: "Link inválido",
          message: "Confira o link recebido ou peça um novo à clínica.",
        });
      } else {
        setState({
          kind: "error",
          title: "Algo deu errado",
          message: result.status === 429 ? RATE_LIMIT_MESSAGE : GENERIC_MESSAGE,
        });
      }
    });
    return () => {
      active = false;
    };
  }, [token]);

  const onSignature = useCallback((value: string | null) => setSignature(value), []);

  const enterFullscreen = () => {
    void document.documentElement.requestFullscreen?.().catch(() => undefined);
  };

  if (state.kind === "loading") {
    return (
      <div className="flex h-full items-center justify-center text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin" />
      </div>
    );
  }
  if (state.kind === "error") return <StatusCard title={state.title} text={state.message} />;
  if (state.kind === "done") {
    return (
      <StatusCard
        done
        title="Obrigado!"
        text={
          reception
            ? "Suas respostas foram enviadas. Por favor, devolva o tablet à recepção."
            : "Suas respostas foram enviadas à clínica. Você já pode fechar esta página."
        }
      />
    );
  }

  const { form } = state;
  const sections = orderedSections(form.form.sections, false);

  const submit = async () => {
    setError(null);
    const missing = missingRequired(sections, answers);
    if (missing.length > 0) {
      setError(`Responda as perguntas obrigatórias: ${missing.slice(0, 3).join("; ")}.`);
      return;
    }
    if (!signature) {
      setError("Assine no quadro antes de enviar.");
      return;
    }
    setSubmitting(true);
    const result = await PublicFormService.submit(token, { answers, signature, website });
    setSubmitting(false);
    if (result.ok) {
      setState({ kind: "done" });
      window.scrollTo({ top: 0 });
    } else if (result.status === 410) {
      setState({
        kind: "error",
        title: "Formulário indisponível",
        message: result.message ?? "Este formulário já foi respondido ou o link expirou.",
      });
    } else {
      setError(result.status === 429 ? RATE_LIMIT_MESSAGE : (result.message ?? GENERIC_MESSAGE));
    }
  };

  return (
    <Card className="mx-auto w-full max-w-2xl border-crm-border bg-crm-surface">
      <CardContent className="space-y-6 pt-5 sm:pt-6">
        <div className="flex items-start justify-between gap-3">
          <div className="space-y-1">
            <p className="text-sm font-medium text-primary">{form.clinicName}</p>
            <h1 className="text-xl font-semibold">{form.formName}</h1>
            <p className="text-sm text-muted-foreground">
              Olá, {form.patientFirstName}! Responda com atenção. Campos com * são obrigatórios.
            </p>
          </div>
          {reception && (
            <Button type="button" variant="ghost" size="icon" onClick={enterFullscreen}>
              <Maximize2 className="h-4 w-4" />
              <span className="sr-only">Tela cheia</span>
            </Button>
          )}
        </div>

        <AnamnesisFields sections={sections} answers={answers} onChange={setAnswers} size="large" />

        {/* Honeypot: invisível para pessoas; robôs costumam preencher. */}
        <input
          type="text"
          name="website"
          tabIndex={-1}
          autoComplete="off"
          aria-hidden="true"
          className="hidden"
          value={website}
          onChange={(e) => setWebsite(e.target.value)}
        />

        <div className="space-y-2">
          <Label className="text-base">
            Assinatura <span className="text-destructive">*</span>
          </Label>
          <p className="text-sm text-muted-foreground">
            Declaro que as informações acima são verdadeiras.
          </p>
          <SignaturePad onChange={onSignature} />
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}

        <Button
          type="button"
          size="lg"
          className="w-full"
          disabled={submitting}
          onClick={() => void submit()}
        >
          {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Enviar respostas
        </Button>
      </CardContent>
    </Card>
  );
}
