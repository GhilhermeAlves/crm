export type InvitationStatus = "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";

export type Invitation = {
  id: string;
  companyId: string;
  email: string;
  inviteeName: string | null;
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

/** GET /invitations/preview (público) — espelha InvitationPreviewResponse do backend. */
export type InvitationPreview = {
  companyName: string;
  email: string;
  inviteeName: string | null;
  role: string;
  /** Status efetivo: PENDING vencido chega como EXPIRED. */
  status: InvitationStatus;
  expiresAt: string;
  hasAccount: boolean;
};

/**
 * POST /invitations/register (público). Sem e-mail, empresa, perfil ou
 * permissões: tudo isso vem do convite, no servidor.
 */
export type InvitationRegisterRequest = {
  token: string;
  name: string;
  password: string;
};

export const INVITATION_ROLES = ["ADMIN", "MANAGER", "AGENT", "VIEWER"] as const;
