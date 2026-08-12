export type InvitationStatus = "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";

export type Invitation = {
  id: string;
  companyId: string;
  email: string;
  role: string;
  status: InvitationStatus;
  invitedBy: string | null;
  expiresAt: string;
  createdAt: string;
};

export type CreateInvitationRequest = {
  email: string;
  role: string;
};

export const INVITATION_ROLES = ["ADMIN", "MANAGER", "AGENT", "VIEWER"] as const;
