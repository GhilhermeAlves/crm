export type FollowUpSequenceStatus = "ACTIVE" | "INACTIVE";

export type FollowUpSequence = {
  id: string;
  name: string;
  description: string | null;
  status: FollowUpSequenceStatus;
  createdAt: string;
  updatedAt: string;
};

export type FollowUpSequenceRequest = {
  name: string;
  description?: string;
};

export const FOLLOW_UP_SEQUENCE_STATUS_LABELS: Record<FollowUpSequenceStatus, string> = {
  ACTIVE: "Ativa",
  INACTIVE: "Inativa",
};
