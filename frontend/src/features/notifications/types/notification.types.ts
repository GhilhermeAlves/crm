export type NotificationType =
  | "TASK"
  | "WORKFLOW"
  | "INVITATION"
  | "MESSAGE"
  | "LEAD"
  | "OPPORTUNITY"
  | "SYSTEM"
  | "INFO";

export interface Notification {
  id: string;
  companyId: string;
  userId: string;
  type: NotificationType;
  title: string;
  body: string | null;
  metadata: string | null;
  readAt: string | null;
  read: boolean;
  createdAt: string;
}