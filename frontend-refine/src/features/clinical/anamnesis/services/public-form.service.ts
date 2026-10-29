import type { AnamnesisModel } from "@/features/masterdata/anamnesis/types/anamnesis.types";
import type { PatientAnamnesisAnswers } from "../types/patient-anamnesis.types";

/** Formulário que o paciente vê pelo link (sem login): nunca a ficha. */
export type PublicForm = {
  clinicName: string;
  /** Data URL ou link do logo da empresa; null quando não cadastrado. */
  clinicLogoUrl: string | null;
  patientFirstName: string;
  formName: string;
  form: AnamnesisModel;
  mode: "LINK" | "RECEPTION";
};

/** O status HTTP decide a tela (404 inválido, 410 usado/expirado, 429 limite). */
export type PublicFormResult<T> =
  { ok: true; data: T } | { ok: false; status: number; message?: string };

const PUBLIC_BASE = "/api/v1/public/forms";

async function toResult<T>(response: Response, parse: boolean): Promise<PublicFormResult<T>> {
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

export const PublicFormService = {
  async get(token: string): Promise<PublicFormResult<PublicForm>> {
    const response = await fetch(`${PUBLIC_BASE}/${encodeURIComponent(token)}`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
    return toResult<PublicForm>(response, true);
  },

  async submit(
    token: string,
    payload: { answers: PatientAnamnesisAnswers; signature: string; website: string },
  ): Promise<PublicFormResult<void>> {
    const response = await fetch(`${PUBLIC_BASE}/${encodeURIComponent(token)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload),
    });
    return toResult<void>(response, false);
  },
};
