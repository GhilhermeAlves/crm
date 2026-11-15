/**
 * Normalização de erros HTTP (padrão do data-provider do Portal Praia).
 *
 * Converte qualquer erro do Axios/Spring num formato único, para telas e
 * mutations não precisarem conhecer `error.response.data.*`. O interceptor do
 * `api` continua rejeitando o erro original do Axios (há código que lê
 * `error.response.status`); use `toHttpError(error)` onde precisar da forma
 * normalizada.
 */
export type HttpError = {
  /** Status HTTP; 0 quando não houve resposta (rede/timeout). */
  status: number;
  message: string;
  /** Código de negócio do backend (ex.: BILLING_BLOCKED, CRM_ACCESS_DENIED). */
  code?: string;
  /** Erros de validação por campo. */
  errors?: Record<string, string>;
};

type ErrorBody = {
  message?: unknown;
  detail?: unknown;
  error?: unknown;
  code?: unknown;
  errors?: unknown;
  fieldErrors?: unknown;
};

const FALLBACK_MESSAGE = "Algo deu errado";
const NETWORK_MESSAGE = "Falha de conexão. Verifique sua rede e tente novamente.";

function asString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim() ? value : undefined;
}

/** Aceita `{campo: "msg"}`, `{campo: ["msg"]}` ou `[{field, message}]`. */
function normalizeFieldErrors(raw: unknown): Record<string, string> | undefined {
  if (!raw || typeof raw !== "object") return undefined;
  const out: Record<string, string> = {};
  if (Array.isArray(raw)) {
    for (const item of raw) {
      const field = asString((item as { field?: unknown })?.field);
      const message = asString((item as { message?: unknown })?.message);
      if (field && message) out[field] = message;
    }
  } else {
    for (const [field, value] of Object.entries(raw)) {
      const message = Array.isArray(value) ? asString(value[0]) : asString(value);
      if (message) out[field] = message;
    }
  }
  return Object.keys(out).length ? out : undefined;
}

export function toHttpError(error: unknown, fallbackMessage = FALLBACK_MESSAGE): HttpError {
  const err = error as {
    response?: { status?: number; data?: unknown };
    request?: unknown;
    message?: unknown;
  };

  if (err?.response) {
    const status = err.response.status ?? 0;
    const body = (
      err.response.data && typeof err.response.data === "object" ? err.response.data : {}
    ) as ErrorBody;
    return {
      status,
      message:
        asString(body.message) ?? asString(body.detail) ?? asString(body.error) ?? fallbackMessage,
      code: asString(body.code),
      errors: normalizeFieldErrors(body.errors ?? body.fieldErrors),
    };
  }

  if (err?.request) {
    return { status: 0, message: NETWORK_MESSAGE };
  }

  return { status: 0, message: asString(err?.message) ?? fallbackMessage };
}
