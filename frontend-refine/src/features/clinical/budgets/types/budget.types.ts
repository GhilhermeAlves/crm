import type { PaymentMethod } from "@/features/finance/types/finance.types";
import type {
  ProcedureTooth,
  TransactionStatus,
} from "@/features/clinical/procedures/types/procedure.types";

export type { PaymentMethod };
export type BudgetStatus = "DRAFT" | "APPROVED" | "LOST";

export type BudgetRequest = {
  title: string;
  procedureIds: string[];
  discountCents: number;
  notes?: string | null;
  itemPrices?: Record<string, number>;
};

export type ApproveBudgetRequest = {
  paymentMethod: PaymentMethod;
  installments: number;
  firstDueDate: string;
};

export type BudgetSummary = {
  id: string;
  title: string;
  status: BudgetStatus;
  subtotalCents: number;
  discountCents: number;
  totalCents: number;
  updatedAt: string;
  updatedByName: string | null;
};

export type BudgetItem = {
  procedureId: string;
  name: string;
  teeth: ProcedureTooth[];
  professionalName: string | null;
  priceCents: number;
};

export type BudgetInstallment = {
  id: string;
  dueDate: string;
  amountCents: number;
  status: TransactionStatus;
};

export type BudgetDetail = {
  id: string;
  contactId: string;
  title: string;
  status: BudgetStatus;
  subtotalCents: number;
  discountCents: number;
  totalCents: number;
  notes: string | null;
  paymentMethod: PaymentMethod | null;
  installments: number | null;
  firstDueDate: string | null;
  approvedAt: string | null;
  lostAt: string | null;
  items: BudgetItem[];
  installmentsList: BudgetInstallment[];
  updatedAt: string;
  updatedByName: string | null;
};
