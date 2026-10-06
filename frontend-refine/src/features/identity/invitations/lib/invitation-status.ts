import type { InvitationStatus } from "../types/invitation.types";

type InvitationStatusMeta = {
  label: string;
  variant: "default" | "secondary" | "destructive" | "outline";
};

/**
 * Status EFETIVO do convite (o backend já devolve EXPIRED para PENDING vencido).
 * Inativos = EXPIRED | REVOKED; Pendentes = PENDING.
 */
export const INVITATION_STATUS_LABEL: Record<InvitationStatus, InvitationStatusMeta> = {
  PENDING: { label: "Pendente", variant: "secondary" },
  ACCEPTED: { label: "Aceito", variant: "default" },
  REVOKED: { label: "Revogado", variant: "destructive" },
  EXPIRED: { label: "Expirado", variant: "outline" },
};

export function invitationStatusMeta(status: InvitationStatus): InvitationStatusMeta {
  return INVITATION_STATUS_LABEL[status] ?? { label: status, variant: "secondary" };
}

/** Convite que ainda pode ser reenviado/revogado. */
export function isUsableInvitation(status: InvitationStatus): boolean {
  return status === "PENDING" || status === "EXPIRED";
}
