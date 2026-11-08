export type BillingStatus =
  "NOT_STARTED" | "SYNC_PENDING" | "ACTIVE" | "PAST_DUE" | "BLOCKED" | "EXEMPT";

export type InvoiceStatus =
  "PENDING" | "CONFIRMED" | "RECEIVED" | "OVERDUE" | "REFUNDED" | "CANCELLED";

export type BillingView = {
  status: BillingStatus;
  plan: string;
  monthlyPriceCents: number;
  nextDueDate: string | null;
  syncError: string | null;
  hasSubscription: boolean;
  overdueSince: string | null;
  graceUntil: string | null;
  openInvoiceUrl: string | null;
};

export type InvoiceView = {
  id: string;
  dueDate: string;
  valueCents: number;
  status: InvoiceStatus;
  invoiceUrl: string | null;
  paidAt: string | null;
};

export type CompanyBillingStatus = { companyId: string; status: BillingStatus | "UNKNOWN" };

export const BILLING_STATUS_LABELS: Record<BillingStatus | "UNKNOWN", string> = {
  NOT_STARTED: "Não iniciada",
  SYNC_PENDING: "Pendente de envio",
  ACTIVE: "Em dia",
  PAST_DUE: "Vencida",
  BLOCKED: "Bloqueada",
  EXEMPT: "Isenta",
  UNKNOWN: "—",
};

export const INVOICE_STATUS_LABELS: Record<InvoiceStatus, string> = {
  PENDING: "Aberta",
  CONFIRMED: "Confirmada",
  RECEIVED: "Paga",
  OVERDUE: "Vencida",
  REFUNDED: "Estornada",
  CANCELLED: "Cancelada",
};
