"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { AlertCircle, ArrowLeft, Loader2, Phone, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PhoneOtpService, PhoneOtpError } from "../services/phone-otp.service";
import { loginWithGateway } from "@/lib/gateway-auth";

/**
 * Login por telefone/OTP (Sprint 7.4).
 *
 * <p>O telefone NÃO é um Identity Provider do Keycloak: a confirmação do OTP
 * acontece no crm-backend (endpoint público) e comprova a POSSE do telefone.
 * Após confirmado, a UI segue para o fluxo de senha do Keycloak
 * ({@code loginWithGateway}) — a sessão real é criada pelo Access Gateway como
 * em qualquer outro login, de forma que o falar /api do dashboard funciona.
 * Exige que a conta tenha senha configurada no Keycloak.
 */
export type PhoneLoginFormProps = {
  redirect?: string;
  onBack: () => void;
};

export function PhoneLoginForm({ redirect, onBack }: PhoneLoginFormProps) {
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [step, setStep] = useState<"phone" | "otp">("phone");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  async function handleSendOtp(event: FormEvent) {
    event.preventDefault();
    if (!phone.trim()) {
      setError("Informe seu telefone com DDI (ex.: +55 11 99999-0000).");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    setInfo(null);
    try {
      const result = await PhoneOtpService.sendOtp(phone.trim());
      if (!result.sent) {
        setError("Não foi possível enviar o código. Aguarde o cooldown e tente novamente.");
        return;
      }
      setInfo(
        `Código enviado para ${result.phoneE164 ?? phone.trim()}. Você pode reenviar em ${result.resendCooldownSeconds}s.`,
      );
      setStep("otp");
    } catch (caught) {
      if (caught instanceof PhoneOtpError) {
        setError(messageForError(caught.errorCode, caught.message));
      } else {
        setError("Não foi possível enviar o código. Tente novamente.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleConfirmOtp(event: FormEvent) {
    event.preventDefault();
    if (!otp.trim()) {
      setError("Digite o código de 6 dígitos enviado.");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      const result = await PhoneOtpService.verifyOtp(phone.trim(), otp.trim());
      if (!result.userExists) {
        setError("Telefone não encontrado. Verifique o número ou cadastre uma conta.");
        return;
      }
      // Telefone confirmado. Segue para o fluxo de senha do Keycloak — o
      // gateway cria a sessão real (credentials/provedor) e restaura o redirect.
      loginWithGateway(redirect);
    } catch (caught) {
      if (caught instanceof PhoneOtpError) {
        setError(messageForError(caught.errorCode, caught.message));
      } else {
        setError("Não foi possível validar o código. Tente novamente.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={step === "phone" ? handleSendOtp : handleConfirmOtp} className="space-y-4">
      <div className="rounded-lg border border-crm-border bg-crm-background p-3 text-sm">
        <p className="flex items-center gap-2 font-medium text-crm-text">
          <ShieldCheck className="h-4 w-4 shrink-0 text-crm-primary" />
          Entrar com telefone
        </p>
        <p className="mt-1 text-crm-text-secondary">
          Confirme a posse do seu telefone com um código enviado por SMS. Na
          próxima etapa você fará login com a senha configurada (Keycloak).
        </p>
      </div>

      {step === "phone" ? (
        <label className="block space-y-1.5">
          <span className="text-sm font-medium text-crm-text">Telefone</span>
          <Input
            type="tel"
            inputMode="tel"
            autoComplete="tel"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            placeholder="+55 11 99999-0000"
            disabled={isSubmitting}
            required
          />
        </label>
      ) : (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <button
              type="button"
              onClick={() => {
                setStep("phone");
                setError(null);
                setInfo(null);
              }}
              className="inline-flex items-center gap-1 text-sm font-medium text-crm-primary hover:underline"
            >
              <ArrowLeft className="h-4 w-4" />
              Alterar telefone
            </button>
          </div>
          <label className="block space-y-1.5">
            <span className="text-sm font-medium text-crm-text">Código OTP</span>
            <Input
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              value={otp}
              onChange={(event) => setOtp(event.target.value)}
              placeholder="000000"
              maxLength={6}
              disabled={isSubmitting}
              required
            />
          </label>
        </div>
      )}

      {info ? (
        <p role="status" className="text-sm text-crm-text-secondary">
          {info}
        </p>
      ) : null}
      {error ? (
        <p
          role="alert"
          className="flex items-start gap-2 text-sm text-destructive"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          {error}
        </p>
      ) : null}

      <Button type="submit" variant="crm" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : step === "phone" ? (
          <ShieldCheck className="h-4 w-4" />
        ) : (
          <ShieldCheck className="h-4 w-4" />
        )}
        {step === "phone" ? "Enviar código" : "Confirmar e continuar"}
      </Button>

      <Link
        href="/login"
        onClick={onBack}
        className="block text-center text-sm font-medium text-crm-primary hover:underline"
      >
        Voltar para os provedores
      </Link>
    </form>
  );
}

function messageForError(code: string, fallback: string): string {
  switch (code) {
    case "OTP_EXPIRED":
      return "Código expirado. Solicite um novo.";
    case "OTP_MAX_ATTEMPTS":
      return "Muitas tentativas inválidas. Solicite um novo código.";
    case "OTP_ALREADY_USED":
      return "Código já utilizado. Solicite um novo código.";
    case "OTP_NOT_FOUND":
      return "Nenhum código pendente para esse telefone. Solicite um novo.";
    case "INVALID_PHONE":
      return "Número de telefone inválido. Verifique o formato.";
    case "SEND_OTP_COOLDOWN":
    case "RATE_LIMIT_EXCEEDED":
      return "Muitas solicitações. Aguarde alguns instantes e tente novamente.";
    case "USER_NOT_FOUND":
      return "Nenhuma conta encontrada para este telefone.";
    default:
      return fallback;
  }
}