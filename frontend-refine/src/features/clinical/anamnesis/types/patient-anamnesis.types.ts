import type { AnamnesisModel } from "@/features/masterdata/anamnesis/types/anamnesis.types";

export type PatientAnamnesisStatus = "DRAFT" | "COMPLETED";

/**
 * Resposta de uma pergunta, no formato do backend: valor livre (texto, número,
 * "YES"/"NO", lista de valores das opções) e complemento opcional.
 */
export type PatientAnamnesisAnswer = {
  value: unknown;
  complement: string | null;
};

/** Respostas indexadas pelo id da pergunta (Map<UUID, ...> no backend). */
export type PatientAnamnesisAnswers = Record<string, PatientAnamnesisAnswer>;

export type PatientAnamnesisResponse = {
  id: string;
  contactId: string;
  modelId: string;
  modelName: string;
  modelVersion: number;
  status: PatientAnamnesisStatus;
  answers: PatientAnamnesisAnswers | null;
  snapshot: AnamnesisModel;
  alerts: string[];
  createdBy: string | null;
  createdByName: string | null;
  completedBy: string | null;
  completedByName: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type StartPatientAnamnesisRequest = {
  modelId: string;
};

export type SavePatientAnamnesisRequest = {
  answers: PatientAnamnesisAnswers;
  complete: boolean;
};
