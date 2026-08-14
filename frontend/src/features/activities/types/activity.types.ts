export type ActivityType =
  | "CALL"
  | "MEETING"
  | "EMAIL"
  | "MESSAGE"
  | "NOTE"
  | "PROPOSAL"
  | "FOLLOW_UP"
  | "OTHER";

export type Activity = {
  id: string;
  companyId: string;
  contactId: string | null;
  opportunityId: string | null;
  type: ActivityType;
  subject: string;
  description: string | null;
  activityAt: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateActivityRequest = {
  contactId?: string;
  opportunityId?: string;
  type: ActivityType;
  subject: string;
  description?: string;
  activityAt?: string;
};

export type UpdateActivityRequest = {
  type: ActivityType;
  subject?: string;
  description?: string;
  activityAt?: string;
};

export const ACTIVITY_TYPE_LABELS: Record<ActivityType, string> = {
  CALL: "Ligação",
  MEETING: "Reunião",
  EMAIL: "E-mail",
  MESSAGE: "Mensagem",
  NOTE: "Anotação",
  PROPOSAL: "Proposta",
  FOLLOW_UP: "Follow-up",
  OTHER: "Outro",
};

export const ACTIVITY_TYPE_ICONS: Record<ActivityType, string> = {
  CALL: "📞",
  MEETING: "🤝",
  EMAIL: "✉️",
  MESSAGE: "💬",
  NOTE: "📝",
  PROPOSAL: "📄",
  FOLLOW_UP: "🔁",
  OTHER: "📌",
};
