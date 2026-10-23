import api from "@/lib/api";

export type RegistrationLink = { url: string; expiresAt: string };

export type RegistrationPreview = {
  companyName: string;
  firstName: string;
  lastName: string | null;
  phone: string | null;
};

export type PatientRegistrationPayload = {
  firstName: string;
  lastName: string | null;
  cpf: string | null;
  birthDate: string | null;
  email: string | null;
  phone: string | null;
  address: {
    zipCode: string | null;
    street: string | null;
    number: string | null;
    complement: string | null;
    neighborhood: string | null;
    city: string | null;
    state: string | null;
  };
  /** Honeypot: sempre vazio para humanos. */
  website: string;
};

const PUBLIC_BASE = "/api/v1/public/registration";

/** Resultado da chamada pública: o status HTTP decide a tela (404/410/429). */
export type PublicResult<T> =
  { ok: true; data: T } | { ok: false; status: number; message?: string };

async function toResult<T>(response: Response, parse: boolean): Promise<PublicResult<T>> {
  if (response.ok) {
    return { ok: true, data: (parse ? await response.json() : undefined) as T };
  }
  let message: string | undefined;
  try {
    const body = await response.json();
    message = typeof body?.message === "string" ? body.message : undefined;
  } catch {
    message = undefined;
  }
  return { ok: false, status: response.status, message };
}

export const RegistrationService = {
  /** Geração do link (autenticada, cliente padrão). */
  async createLink(companyId: string, contactId: string): Promise<RegistrationLink> {
    const response = await api.post<RegistrationLink>(
      `/companies/${companyId}/patients/${contactId}/registration-link`,
    );
    return response.data;
  },

  /** Rotas públicas: `fetch` direto, sem o cliente autenticado (sem cookies/refresh/redirect ao login). */
  async preview(token: string): Promise<PublicResult<RegistrationPreview>> {
    const response = await fetch(`${PUBLIC_BASE}/${encodeURIComponent(token)}`, {
      headers: { Accept: "application/json" },
    });
    return toResult<RegistrationPreview>(response, true);
  },

  async submit(token: string, payload: PatientRegistrationPayload): Promise<PublicResult<void>> {
    const response = await fetch(`${PUBLIC_BASE}/${encodeURIComponent(token)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload),
    });
    return toResult<void>(response, false);
  },
};
