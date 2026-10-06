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
  /** Opcional: aparece na lista de pendentes e pré-preenche o cadastro. */
  name?: string;
};

/**
 * POST /companies/{id}/invitations/{id}/regenerate → InvitationLinkResponse.
 * `url` é montada no backend; o token nunca é exposto em campo próprio.
 * Regenerar invalida o link anterior (inclusive em "copiar link").
 */
export type InvitationLink = {
  invitation: Invitation;
  url: string;
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
