export type PatientEvolutionStatus = "DRAFT" | "FINALIZED";

export type PatientEvolutionResponse = {
  id: string;
  contactId: string;
  appointmentId: string | null;
  body: string;
  status: PatientEvolutionStatus;
  createdBy: string | null;
  createdByName: string | null;
  finalizedBy: string | null;
  finalizedByName: string | null;
  finalizedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type PatientEvolutionRequest = {
  body: string;
  appointmentId?: string | null;
};
