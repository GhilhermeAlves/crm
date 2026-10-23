import type { AnamnesisModel } from "@/features/masterdata/anamnesis/types/anamnesis.types";

export type PatientAnamnesisStatus = "DRAFT" | "COMPLETED";

export type PatientAnamnesisAnswer = {
  questionId: string;
  valueText: string | null;
  valueJson: unknown;
  complement: string | null;
};

export type PatientAnamnesisResponse = {
  id: string;
  contactId: string;
  modelId: string;
  modelName: string;
  modelVersion: number;
  status: PatientAnamnesisStatus;
  answers: PatientAnamnesisAnswer[];
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
  answers: PatientAnamnesisAnswer[];
  complete: boolean;
};
