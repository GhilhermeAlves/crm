import { SEGMENT_OPTIONS } from "@/features/identity/modules/types/modules.types";

/** Papéis que o backend aceita em convites (InvitationService.ALLOWED_ROLES). */
export const INVITABLE_ROLES = ["ADMIN", "MANAGER", "AGENT", "VIEWER"] as const;

/** Nome amigável dos papéis de membership. Chave = valor enviado ao backend. */
export const ROLE_LABEL: Record<string, string> = {
  CRM_ADMIN: "Administrador do CRM",
  SUPER_ADMIN: "Super administrador",
  OWNER: "Proprietário",
  ADMIN: "Administrador",
  MANAGER: "Gerente",
  AGENT: "Agente",
  VIEWER: "Visualizador",
};

export function roleLabel(role: string | null | undefined): string {
  if (!role) return "—";
  return ROLE_LABEL[role] ?? role.replace(/_/g, " ");
}

/** Especialidades aceitas no convite (ProfessionalSpecialty no backend). */
export const SPECIALTY_LABEL: Record<string, string> = Object.fromEntries(
  SEGMENT_OPTIONS.map((option) => [option.value, option.label]),
);

export function specialtyLabel(specialty: string | null | undefined): string | null {
  if (!specialty) return null;
  return SPECIALTY_LABEL[specialty] ?? specialty;
}

export const MEMBER_STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Ativo",
  REMOVED: "Inativo",
  PENDING: "Pendente",
};

export function memberStatusLabel(status: string): string {
  return MEMBER_STATUS_LABEL[status] ?? status;
}

/** Perfis com nível administrativo (espelha Membership.isAdminLevelRole). */
const ADMIN_LEVEL_ROLES = new Set(["ADMIN", "OWNER", "SUPER_ADMIN"]);

export function isAdminLevelRole(role: string | null | undefined): boolean {
  return !!role && ADMIN_LEVEL_ROLES.has(role);
}
