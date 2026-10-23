import type { BudgetStatus } from "@/features/clinical/budgets/types/budget.types";

export type ProcedureState = "PLANNED" | "DONE" | "PRE_EXISTING";
export type Dentition = "PERMANENT" | "DECIDUOUS";
export type ToothFace = "M" | "D" | "O" | "V" | "L";
export type TransactionStatus = "PENDING" | "PAID" | "CANCELLED";

export type ProcedureTooth = { tooth: number; faces: ToothFace[] };

export type PatientProcedureResponse = {
  id: string;
  contactId: string;
  catalogItemId: string;
  name: string;
  category: string | null;
  state: ProcedureState;
  dentition: Dentition;
  priceCents: number;
  professionalId: string | null;
  professionalName: string | null;
  notes: string | null;
  performedAt: string | null;
  teeth: ProcedureTooth[];
  transaction: { id: string; status: TransactionStatus } | null;
  createdAt: string;
  updatedAt: string;
  budget: { id: string; title: string; status: BudgetStatus } | null;
  generateTransactionOnDone: boolean;
};

export type PatientProcedureRequest = {
  catalogItemId: string;
  state: ProcedureState;
  dentition: Dentition;
  priceCents: number;
  professionalId?: string | null;
  notes?: string | null;
  performedAt?: string | null;
  teeth: ProcedureTooth[];
  generateTransaction: boolean;
  dueDate?: string | null;
};

export type CompleteProcedureRequest = {
  performedAt: string;
  professionalId?: string | null;
  generateTransaction: boolean;
  dueDate?: string | null;
};

export type ProcedureFilters = { state?: ProcedureState; category?: string; tooth?: number };
