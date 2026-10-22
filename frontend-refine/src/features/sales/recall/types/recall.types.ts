export type RecallStatus = "PENDING" | "SCHEDULED" | "DISMISSED";

export type RecallWindow = "TODAY" | "NEXT_7_DAYS" | "NEXT_30_DAYS" | "OVERDUE" | "ALL";

export type RecallDueUnit = "DAYS" | "WEEKS" | "MONTHS" | "YEARS";

export type Recall = {
  id: string;
  companyId: string;
  contactId: string;
  phone: string | null;
  hostId: string;
  /** Texto do marcador (mantido em sincronia com o nome do marcador cadastrado). */
  tag: string | null;
  tagId: string | null;
  dueDate: string;
  status: RecallStatus;
  appointmentId: string | null;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
};

export type RecallSuggestion = {
  contactId: string;
  contactName: string;
  mobile: string | null;
  phone: string | null;
  hostId: string;
  lastAppointmentAt: string;
  recentProcedures: string[];
};

export type CreateRecallRequest = {
  contactId: string;
  phone: string;
  hostId: string;
  tagId?: string;
  dueInAmount: number;
  dueInUnit: RecallDueUnit;
};

export type UpdateRecallRequest = Partial<CreateRecallRequest>;

export type RecallPageResponse<T> = {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export const RECALL_WINDOW_LABELS: Record<RecallWindow, string> = {
  TODAY: "Vence hoje",
  NEXT_7_DAYS: "Em 7 dias",
  NEXT_30_DAYS: "Em 30 dias",
  OVERDUE: "Vencidos",
  ALL: "Todos",
};

export const RECALL_DUE_UNIT_LABELS: Record<RecallDueUnit, string> = {
  DAYS: "dias",
  WEEKS: "semanas",
  MONTHS: "meses",
  YEARS: "anos",
};
