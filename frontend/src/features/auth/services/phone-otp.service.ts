import api from "@/lib/api";
import type { AxiosError } from "axios";

/**
 * Serviço do login por telefone/OTP (Sprint 7.4).
 *
 * <p>O telefone NÃO é um Identity Provider do Keycloak: é um fluxo local do
 * crm-backend (endpoints públicos `/api/v1/auth/phone/*`) que confirma a POSSE
 * do telefone via código OTP. Após a confirmação a UI segue para o fluxo de
 * senha do Keycloak (a sessão é criada pelo gateway). Este serviço apenas
 * encapsula as chamadas ao backend crm.
 */

export type SendOtpResult = {
  sent: boolean;
  phoneE164?: string;
  ttlSeconds: number;
  resendCooldownSeconds: number;
};

export type VerifyOtpResult = {
  success: boolean;
  errorCode?: string;
  userExists: boolean;
  userId?: string;
  email?: string;
  phoneVerified: boolean;
  message: string;
};

export class PhoneOtpError extends Error {
  readonly errorCode: string;

  constructor(errorCode: string, message: string) {
    super(message);
    this.name = "PhoneOtpError";
    this.errorCode = errorCode;
  }
}

function errorFrom(caught: unknown, fallbackCode: string, fallbackMessage: string): PhoneOtpError {
  const axiosError = caught as AxiosError<{ code?: string; message?: string }>;
  const code = axiosError?.response?.data?.code ?? fallbackCode;
  const message = axiosError?.response?.data?.message ?? fallbackMessage;
  return new PhoneOtpError(code, message);
}

export const PhoneOtpService = {
  /**
   * Solicita o envio de um OTP para o telefone informado.
   */
  async sendOtp(phone: string): Promise<SendOtpResult> {
    try {
      const response = await api.post<SendOtpResult>("/auth/phone/send-otp", { phone });
      if (!response.data.sent) {
        throw new PhoneOtpError("SEND_OTP_COOLDOWN", "Aguarde antes de solicitar outro código.");
      }
      return response.data;
    } catch (caught) {
      if (caught instanceof PhoneOtpError) throw caught;
      const axiosError = caught as AxiosError<SendOtpResult>;
      if (axiosError?.response?.data?.sent === false) {
        throw new PhoneOtpError(
          "SEND_OTP_COOLDOWN",
          "Aguarde antes de solicitar outro código.",
        );
      }
      throw errorFrom(caught, "SEND_OTP_FAILED", "Não foi possível enviar o código. Tente novamente.");
    }
  },

  /**
   * Valida o OTP e confirma a posse do telefone. O gateway não participa
   * deste passo — o OTP é validado no crm-backend (endpoint público).
   */
  async verifyOtp(phone: string, otp: string): Promise<VerifyOtpResult> {
    try {
      const response = await api.post<VerifyOtpResult>("/auth/phone/verify-otp", { phone, otp });
      if (!response.data.success) {
        throw new PhoneOtpError(
          response.data.errorCode ?? "OTP_INVALID",
          response.data.message || "Código inválido.",
        );
      }
      return response.data;
    } catch (caught) {
      if (caught instanceof PhoneOtpError) throw caught;
      throw errorFrom(caught, "OTP_INVALID", "Não foi possível validar o código.");
    }
  },
};