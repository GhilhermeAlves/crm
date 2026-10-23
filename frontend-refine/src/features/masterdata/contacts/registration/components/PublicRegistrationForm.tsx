"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { CheckCircle2, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatCpf, validateCpf } from "@/lib/format";
import { maskCep, maskPhone, onlyDigits } from "@/lib/masks";
import {
  RegistrationService,
  type PatientRegistrationPayload,
  type RegistrationPreview,
} from "../registration.service";

type Screen =
  | { kind: "loading" }
  | { kind: "form"; preview: RegistrationPreview }
  | { kind: "done" }
  | { kind: "expired" }
  | { kind: "notFound" }
  | { kind: "error"; message: string };

type FormState = {
  firstName: string;
  lastName: string;
  cpf: string;
  birthDate: string;
  email: string;
  phone: string;
  zipCode: string;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
  website: string;
};

const EMPTY_FORM: FormState = {
  firstName: "",
  lastName: "",
  cpf: "",
  birthDate: "",
  email: "",
  phone: "",
  zipCode: "",
  street: "",
  number: "",
  complement: "",
  neighborhood: "",
  city: "",
  state: "",
  website: "",
};

const EXPIRED_MESSAGE = "Este link expirou ou já foi usado.";
const NOT_FOUND_MESSAGE = "Link de cadastro não encontrado.";
const RATE_LIMIT_MESSAGE = "Muitas tentativas. Aguarde um instante e tente novamente.";
const GENERIC_MESSAGE = "Não foi possível concluir agora. Tente novamente em instantes.";

function orNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function StatusCard({ title, text, done }: { title: string; text: string; done?: boolean }) {
  return (
    <Card className="m-auto w-full max-w-md border-crm-border bg-crm-surface">
      <CardContent className="space-y-3 pt-6 text-center">
        {done && <CheckCircle2 className="mx-auto h-10 w-10 text-primary" />}
        <h1 className="text-xl font-semibold text-crm-text">{title}</h1>
        <p className="text-sm text-crm-text-secondary">{text}</p>
      </CardContent>
    </Card>
  );
}

/**
 * Cadastro público do paciente (link enviado pela clínica). Usa `fetch` direto nas rotas
 * públicas: não há sessão nem cliente autenticado aqui.
 */
export function PublicRegistrationForm({ token }: { token: string }) {
  const [screen, setScreen] = useState<Screen>({ kind: "loading" });
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const inFlight = useRef(false);

  useEffect(() => {
    let cancelled = false;
    RegistrationService.preview(token)
      .then((result) => {
        if (cancelled) return;
        if (result.ok) {
          const p = result.data;
          setForm((f) => ({
            ...f,
            firstName: p.firstName ?? "",
            lastName: p.lastName ?? "",
            phone: p.phone ? maskPhone(p.phone) : "",
          }));
          setScreen({ kind: "form", preview: p });
        } else if (result.status === 410) {
          setScreen({ kind: "expired" });
        } else if (result.status === 404) {
          setScreen({ kind: "notFound" });
        } else {
          setScreen({
            kind: "error",
            message: result.status === 429 ? RATE_LIMIT_MESSAGE : GENERIC_MESSAGE,
          });
        }
      })
      .catch(() => {
        if (!cancelled) setScreen({ kind: "error", message: GENERIC_MESSAGE });
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const set = (key: keyof FormState, value: string) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: undefined }));
  };

  const validate = (): boolean => {
    const next: Partial<Record<keyof FormState, string>> = {};
    if (!form.firstName.trim()) next.firstName = "Informe o nome";
    if (form.cpf && !validateCpf(form.cpf)) next.cpf = "CPF inválido";
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      next.email = "E-mail inválido";
    }
    if (form.phone) {
      const d = onlyDigits(form.phone);
      if (d.length < 10 || d.length > 11) next.phone = "Celular inválido";
    }
    if (form.state && !/^[A-Za-z]{2}$/.test(form.state.trim())) next.state = "UF com 2 letras";
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (inFlight.current || !validate()) return;
    inFlight.current = true;
    setSubmitting(true);
    setFormError(null);
    const payload: PatientRegistrationPayload = {
      firstName: form.firstName.trim(),
      lastName: orNull(form.lastName),
      cpf: orNull(form.cpf),
      birthDate: orNull(form.birthDate),
      email: orNull(form.email),
      phone: orNull(form.phone),
      address: {
        zipCode: orNull(form.zipCode),
        street: orNull(form.street),
        number: orNull(form.number),
        complement: orNull(form.complement),
        neighborhood: orNull(form.neighborhood),
        city: orNull(form.city),
        state: form.state.trim() ? form.state.trim().toUpperCase() : null,
      },
      website: form.website,
    };
    try {
      const result = await RegistrationService.submit(token, payload);
      if (result.ok) {
        setScreen({ kind: "done" });
      } else if (result.status === 410) {
        setScreen({ kind: "expired" });
      } else if (result.status === 404) {
        setScreen({ kind: "notFound" });
      } else if (result.status === 429) {
        setFormError(RATE_LIMIT_MESSAGE);
      } else {
        setFormError(result.message ?? GENERIC_MESSAGE);
      }
    } catch {
      setFormError(GENERIC_MESSAGE);
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  };

  if (screen.kind === "loading") {
    return (
      <div className="flex h-full items-center justify-center" role="status">
        <Loader2 className="h-6 w-6 animate-spin text-crm-text-secondary" />
        <span className="sr-only">Carregando...</span>
      </div>
    );
  }
  if (screen.kind === "expired") {
    return <StatusCard title="Link indisponível" text={EXPIRED_MESSAGE} />;
  }
  if (screen.kind === "notFound") {
    return <StatusCard title="Link não encontrado" text={NOT_FOUND_MESSAGE} />;
  }
  if (screen.kind === "error") {
    return <StatusCard title="Algo deu errado" text={screen.message} />;
  }
  if (screen.kind === "done") {
    return (
      <StatusCard
        done
        title="Cadastro concluído"
        text="Recebemos seus dados. Obrigado! Você já pode fechar esta página."
      />
    );
  }

  const field = (
    key: keyof FormState,
    label: string,
    opts: {
      type?: string;
      mask?: (v: string) => string;
      inputMode?: "numeric" | "tel" | "email" | "text";
      autoComplete?: string;
      className?: string;
      maxLength?: number;
    } = {},
  ) => (
    <div className={`space-y-1.5 ${opts.className ?? ""}`}>
      <Label htmlFor={`reg-${key}`}>{label}</Label>
      <Input
        id={`reg-${key}`}
        type={opts.type ?? "text"}
        inputMode={opts.inputMode}
        autoComplete={opts.autoComplete}
        maxLength={opts.maxLength}
        value={form[key]}
        onChange={(e) => set(key, opts.mask ? opts.mask(e.target.value) : e.target.value)}
      />
      {errors[key] && <p className="text-sm text-destructive">{errors[key]}</p>}
    </div>
  );

  return (
    <Card className="mx-auto w-full max-w-2xl border-crm-border bg-crm-surface">
      <CardContent className="pt-5 sm:pt-6">
        <div className="mb-5 space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight text-crm-text">
            Complete seu cadastro
          </h1>
          <p className="text-sm text-crm-text-secondary">
            {screen.preview.companyName}: confira e preencha seus dados abaixo.
          </p>
        </div>
        <form onSubmit={handleSubmit} noValidate className="grid gap-4 sm:grid-cols-2">
          {field("firstName", "Nome", { autoComplete: "given-name" })}
          {field("lastName", "Sobrenome", { autoComplete: "family-name" })}
          {field("cpf", "CPF", { mask: formatCpf, inputMode: "numeric" })}
          {field("birthDate", "Data de nascimento", { type: "date", autoComplete: "bday" })}
          {field("email", "E-mail", { type: "email", inputMode: "email", autoComplete: "email" })}
          {field("phone", "Celular", { mask: maskPhone, inputMode: "tel", autoComplete: "tel" })}
          {field("zipCode", "CEP", { mask: maskCep, inputMode: "numeric" })}
          {field("street", "Rua", { autoComplete: "address-line1" })}
          {field("number", "Número", { maxLength: 20 })}
          {field("complement", "Complemento")}
          {field("neighborhood", "Bairro")}
          {field("city", "Cidade")}
          {field("state", "UF", {
            mask: (v) =>
              v
                .replace(/[^A-Za-z]/g, "")
                .slice(0, 2)
                .toUpperCase(),
          })}

          {/* Honeypot: invisível e fora da navegação por teclado; humanos não preenchem. */}
          <div aria-hidden="true" className="absolute left-[-9999px] h-0 w-0 overflow-hidden">
            <label htmlFor="reg-website">Website</label>
            <input
              id="reg-website"
              name="website"
              type="text"
              tabIndex={-1}
              autoComplete="off"
              value={form.website}
              onChange={(e) => set("website", e.target.value)}
            />
          </div>

          {formError && (
            <p className="text-sm text-destructive sm:col-span-2" role="alert">
              {formError}
            </p>
          )}
          <Button type="submit" className="sm:col-span-2" disabled={submitting}>
            {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Enviar cadastro
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
