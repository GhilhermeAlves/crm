export type Contact = {
  id: string;
  companyId: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  notes: string | null;
  createdAt: string;
};

export type CreateContactRequest = {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  notes?: string;
};

export type UpdateContactRequest = {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  notes?: string;
};

// ---------------------------------------------------------------------------
// Customer 360
// ---------------------------------------------------------------------------

export type ContactSummary = {
  id: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  notes: string | null;
  initials: string;
  createdAt: string;
  lastInteractionAt: string;
  atRisk: boolean;
  riskMessage: string | null;
};

export type OpportunityItem = {
  id: string;
  title: string;
  value: number;
  stageName: string;
  probability: number;
  status: "OPEN" | "WON" | "LOST";
  statusLabel: string;
  pipelineName: string;
  assignedTo: string | null;
  expectedCloseDate: string | null;
};

export type TaskItem = {
  id: string;
  title: string;
  status: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
  priority: "LOW" | "MEDIUM" | "HIGH" | null;
  dueAt: string | null;
  assigneeId: string | null;
  completedAt: string | null;
  overdue: boolean;
};

export type TimelineEvent = {
  id: string;
  type:
    | "ACTIVITY"
    | "TASK_CREATED"
    | "TASK_COMPLETED"
    | "OPPORTUNITY_CREATED"
    | "OPPORTUNITY_MOVED"
    | "OPPORTUNITY_WON"
    | "OPPORTUNITY_LOST";
  title: string;
  description: string | null;
  occurredAt: string;
  referenceId: string;
  subject: string;
};

export type NextAction = {
  type:
    | "FOLLOW_UP"
    | "COMPLETE_TASK"
    | "REVIEW_CLOSING"
    | "FORMAL_PROPOSAL"
    | "NONE";
  title: string;
  description: string;
  priority: number;
};

export type Customer360 = {
  companyId: string;
  contact: ContactSummary;
  openOpportunities: number;
  openValue: number;
  opportunities: OpportunityItem[];
  tasks: TaskItem[];
  timeline: TimelineEvent[];
  nextAction: NextAction;
};

export const OPPORTUNITY_STATUS_LABELS: Record<string, string> = {
  OPEN: "Aberta",
  WON: "Ganha",
  LOST: "Perdida",
};

export const TASK_STATUS_LABELS: Record<string, string> = {
  PENDING: "Pendente",
  IN_PROGRESS: "Em andamento",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};

export const TASK_PRIORITY_LABELS: Record<string, string> = {
  LOW: "Baixa",
  MEDIUM: "Média",
  HIGH: "Alta",
};
